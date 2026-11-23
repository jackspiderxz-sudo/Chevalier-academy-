/**
 * Shared Web Media Utilities for Chevalier Academy:
 * 1. Reusable YouTube URL parser, thumbnail, click-to-play embed, and external fallback helpers.
 * 2. Lightweight browser-side image compression before Base64/localStorage storage.
 */

const VIDEO_ID_REGEX = /^[A-Za-z0-9_-]{11}$/;

const ALLOWED_YOUTUBE_HOSTS = new Set([
  'youtube.com',
  'www.youtube.com',
  'm.youtube.com',
  'music.youtube.com',
  'youtube-nocookie.com',
  'www.youtube-nocookie.com'
]);

const ALLOWED_SHORT_HOSTS = new Set([
  'youtu.be',
  'www.youtu.be'
]);

export const DEFAULT_IMAGE_COMPRESSION_OPTIONS = Object.freeze({
  maxDimension: 1280,
  quality: 0.82,
  skipBelowBytes: 120 * 1024,
  preserveOriginalIfSmaller: true
});

export function parseYouTubeUrl(url) {
  const trimmed = String(url || '').trim();
  if (!trimmed) return null;

  let parsed;
  try {
    parsed = new URL(trimmed);
  } catch {
    return null;
  }

  const protocol = parsed.protocol.toLowerCase();
  if (protocol !== 'https:' && protocol !== 'http:') return null;

  const host = parsed.hostname.toLowerCase();
  const segments = parsed.pathname.split('/').filter(Boolean);

  let videoId = null;
  let isShort = false;

  if (ALLOWED_SHORT_HOSTS.has(host)) {
    videoId = segments[0] || null;
  } else if (ALLOWED_YOUTUBE_HOSTS.has(host)) {
    const first = (segments[0] || '').toLowerCase();
    if (first === 'watch') {
      videoId = parsed.searchParams.get('v');
    } else if (first === 'shorts') {
      videoId = segments[1] || null;
      isShort = true;
    } else if (first === 'embed') {
      videoId = segments[1] || null;
    } else {
      return null;
    }
  } else {
    return null;
  }

  const cleanId = String(videoId || '').trim();
  if (!VIDEO_ID_REGEX.test(cleanId)) return null;

  const canonicalWatchUrl = isShort
    ? `https://www.youtube.com/shorts/${cleanId}`
    : `https://www.youtube.com/watch?v=${cleanId}`;

  return {
    videoId: cleanId,
    originalUrl: trimmed,
    isShort,
    canonicalWatchUrl,
    externalFallbackUrl: canonicalWatchUrl,
    thumbnailUrl: getYouTubeThumbnailUrl(cleanId),
    maxResThumbnailUrl: getYouTubeMaxResThumbnailUrl(cleanId),
    clickToPlayEmbedUrl: getYouTubeClickToPlayEmbedUrl(cleanId),
    staticEmbedUrl: getYouTubeStaticEmbedUrl(cleanId)
  };
}

export function isValidYouTubeUrl(url) {
  return parseYouTubeUrl(url) !== null;
}

export function isValidYouTubeShortsUrl(url) {
  return parseYouTubeUrl(url)?.isShort === true;
}

export function extractYouTubeVideoId(url) {
  return parseYouTubeUrl(url)?.videoId || null;
}

export function getYouTubeThumbnailUrl(videoId) {
  return `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
}

export function getYouTubeMaxResThumbnailUrl(videoId) {
  return `https://img.youtube.com/vi/${videoId}/maxresdefault.jpg`;
}

export function getYouTubeClickToPlayEmbedUrl(videoId) {
  return `https://www.youtube-nocookie.com/embed/${videoId}?autoplay=1&rel=0&modestbranding=1&playsinline=1`;
}

export function getYouTubeStaticEmbedUrl(videoId) {
  return `https://www.youtube-nocookie.com/embed/${videoId}?rel=0&modestbranding=1&playsinline=1`;
}

export function buildYouTubeEmbedSpec(url) {
  const parsed = parseYouTubeUrl(url);
  if (!parsed) return null;
  return {
    videoId: parsed.videoId,
    isShort: parsed.isShort,
    thumbnailUrl: parsed.thumbnailUrl,
    maxResThumbnailUrl: parsed.maxResThumbnailUrl,
    clickToPlayEmbedUrl: parsed.clickToPlayEmbedUrl,
    staticEmbedUrl: parsed.staticEmbedUrl,
    externalFallbackUrl: parsed.externalFallbackUrl
  };
}

export function computeScaledDimensions(width, height, maxDimension = DEFAULT_IMAGE_COMPRESSION_OPTIONS.maxDimension) {
  const w = Math.max(1, Math.round(Number(width) || 0));
  const h = Math.max(1, Math.round(Number(height) || 0));
  const limit = Math.max(64, Math.round(Number(maxDimension) || DEFAULT_IMAGE_COMPRESSION_OPTIONS.maxDimension));
  const longest = Math.max(w, h);
  if (longest <= limit) {
    return { width: w, height: h, scaled: false };
  }
  const ratio = limit / longest;
  return {
    width: Math.max(1, Math.round(w * ratio)),
    height: Math.max(1, Math.round(h * ratio)),
    scaled: true
  };
}

export function estimateDataUrlBytes(dataUrl) {
  const str = String(dataUrl || '');
  const commaIdx = str.indexOf(',');
  if (commaIdx < 0) return str.length;
  const payload = str.slice(commaIdx + 1);
  const padding = payload.endsWith('==') ? 2 : payload.endsWith('=') ? 1 : 0;
  return Math.max(0, Math.floor((payload.length * 3) / 4) - padding);
}

export function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(new Error('Failed to read file'));
    reader.readAsDataURL(file);
  });
}

function loadImageFromDataUrl(dataUrl) {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error('Failed to decode image'));
    img.src = dataUrl;
  });
}

/**
 * Lightweight browser-side image compression before Base64/localStorage storage.
 * Preserves the original data URL if:
 * - The file is already small and within maxDimension, or
 * - Canvas APIs are unavailable, or
 * - Re-encoding does not reduce size.
 */
export async function compressImageFileForStorage(file, options = {}) {
  const cfg = { ...DEFAULT_IMAGE_COMPRESSION_OPTIONS, ...options };
  const originalDataUrl = await readFileAsDataUrl(file);
  const mime = String(file?.type || '').toLowerCase();

  const canUseCanvas =
    typeof document !== 'undefined' &&
    typeof Image !== 'undefined' &&
    typeof document.createElement === 'function';

  if (!canUseCanvas || !mime.startsWith('image/') || mime === 'image/svg+xml' || mime === 'image/gif') {
    return {
      dataUrl: originalDataUrl,
      originalDataUrl,
      compressed: false,
      originalBytes: estimateDataUrlBytes(originalDataUrl),
      outputBytes: estimateDataUrlBytes(originalDataUrl)
    };
  }

  try {
    const img = await loadImageFromDataUrl(originalDataUrl);
    const dims = computeScaledDimensions(img.naturalWidth || img.width, img.naturalHeight || img.height, cfg.maxDimension);
    const originalBytes = estimateDataUrlBytes(originalDataUrl);

    if (!dims.scaled && originalBytes <= cfg.skipBelowBytes) {
      return {
        dataUrl: originalDataUrl,
        originalDataUrl,
        compressed: false,
        width: dims.width,
        height: dims.height,
        originalBytes,
        outputBytes: originalBytes
      };
    }

    const canvas = document.createElement('canvas');
    canvas.width = dims.width;
    canvas.height = dims.height;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      return {
        dataUrl: originalDataUrl,
        originalDataUrl,
        compressed: false,
        originalBytes,
        outputBytes: originalBytes
      };
    }

    ctx.drawImage(img, 0, 0, dims.width, dims.height);
    const outputMime = mime === 'image/webp' ? 'image/webp' : 'image/jpeg';
    const compressedDataUrl = canvas.toDataURL(outputMime, cfg.quality);
    const outputBytes = estimateDataUrlBytes(compressedDataUrl);

    if (cfg.preserveOriginalIfSmaller && !dims.scaled && outputBytes >= originalBytes) {
      return {
        dataUrl: originalDataUrl,
        originalDataUrl,
        compressed: false,
        width: dims.width,
        height: dims.height,
        originalBytes,
        outputBytes: originalBytes
      };
    }

    return {
      dataUrl: compressedDataUrl,
      originalDataUrl,
      compressed: true,
      width: dims.width,
      height: dims.height,
      originalBytes,
      outputBytes
    };
  } catch {
    const originalBytes = estimateDataUrlBytes(originalDataUrl);
    return {
      dataUrl: originalDataUrl,
      originalDataUrl,
      compressed: false,
      originalBytes,
      outputBytes: originalBytes
    };
  }
}
