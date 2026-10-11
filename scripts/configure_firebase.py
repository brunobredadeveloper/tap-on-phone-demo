"""CI injects optional public Firebase client configuration from a repository secret."""
import json, os
from pathlib import Path
raw=os.environ.get('FIREBASE_CONFIG_JSON','').strip()
if raw:
 config=json.loads(raw)
 if 'project_info' in config:
  clients=[c for c in config.get('client',[]) if c.get('client_info',{}).get('android_client_info',{}).get('package_name')=='dev.breda.tap']
  if len(clients)!=1:raise ValueError('Expected Android package dev.breda.tap')
  client=clients[0]
  config={'projectId':config['project_info']['project_id'],'applicationId':client['client_info']['mobilesdk_app_id'],'apiKey':client['api_key'][0]['current_key']}
 for field in ['projectId','applicationId','apiKey']:
  if not isinstance(config.get(field),str) or not config[field]:raise ValueError('Missing Firebase field: '+field)
 path=Path('app/src/main/assets/firebase-config.json');path.parent.mkdir(parents=True,exist_ok=True)
 path.write_text(json.dumps(config))
 print('Firebase client configuration included')
else:print('Firebase not configured: building local simulation APK')
