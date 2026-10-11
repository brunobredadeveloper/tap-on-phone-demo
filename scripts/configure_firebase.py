"""CI injects optional public Firebase client configuration from a repository secret."""
import json, os
from pathlib import Path
raw=os.environ.get('FIREBASE_CONFIG_JSON','').strip()
if raw:
 config=json.loads(raw)
 for field in ['projectId','applicationId','apiKey']:
  if not isinstance(config.get(field),str) or not config[field]:raise ValueError('Missing Firebase field: '+field)
 path=Path('app/src/main/assets/firebase-config.json');path.parent.mkdir(parents=True,exist_ok=True)
 path.write_text(json.dumps(config))
 print('Firebase client configuration included')
else:print('Firebase not configured: building local simulation APK')
