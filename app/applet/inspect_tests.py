import xml.etree.ElementTree as ET
import glob
import os

total_tests = 0
total_failures = 0
total_errors = 0
total_skipped = 0
print("=== XML TEST RESULTS INSPECTION ===")
xml_files = sorted(glob.glob('app/build/test-results/testDebugUnitTest/TEST-*.xml'))
print(f"Total XML files found: {len(xml_files)}")
for xml_path in xml_files:
    tree = ET.parse(xml_path)
    root = tree.getroot()
    name = root.get('name', os.path.basename(xml_path))
    tests = int(root.get('tests', 0))
    failures = int(root.get('failures', 0))
    errors = int(root.get('errors', 0))
    skipped = int(root.get('skipped', 0))
    total_tests += tests
    total_failures += failures
    total_errors += errors
    total_skipped += skipped
    status = "PASS" if (failures == 0 and errors == 0) else "FAIL"
    print(f"{name}: tests={tests}, failures={failures}, errors={errors}, skipped={skipped} [{status}]")
    for testcase in root.findall('testcase'):
        failure = testcase.find('failure')
        error = testcase.find('error')
        if failure is not None:
            print(f"  FAILURE in {testcase.get('name')}(): {failure.get('message')}")
        if error is not None:
            print(f"  ERROR in {testcase.get('name')}(): {error.get('message')}")

print(f"\nAGGREGATE: tests={total_tests}, failures={total_failures}, errors={total_errors}, skipped={total_skipped}")
