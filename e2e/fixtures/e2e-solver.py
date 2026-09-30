import json
import sys

# argv[1] = participant output file
# argv[2] = directory of level inputs (unused by this deterministic E2E solver)
# argv[3] = level number
with open(sys.argv[1], 'r', encoding='utf-8') as handle:
    submission = json.load(handle)

score = submission.get('score', 0)
print(json.dumps({
    'score': score,
    'status': 'SCORED',
    'messages': ['Playwright E2E deterministic score']
}))
