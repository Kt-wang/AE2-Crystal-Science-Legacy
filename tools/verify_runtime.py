#!/usr/bin/env python3
"""Run the packaged AE2CSL regression in the mods workspace on formal Cleanroom 0.4.0."""
import argparse, json, os, re, shutil, subprocess, sys, time, traceback
from pathlib import Path
project=Path(__file__).resolve().parents[1]
root=project.parents[1]
sys.path.insert(0,str(root/'1.12.2/mcp-testbridge/tools'))
from mcp_client import Bridge
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('side',choices=['client','server'])
parser.add_argument('--name',required=True,help='Fresh scenario name; existing evidence is never overwritten')
parser.add_argument('--extra',type=Path,action='append',default=[],help='Additional actual installation jar')
parser.add_argument('--bridge-port',type=int,default=18871)
parser.add_argument('--display',default=os.environ.get('DISPLAY',':0'))
parser.add_argument('--loader-version',choices=['0.4.0','0.7.14-alpha'],default='0.4.0')
parser.add_argument('--mod-jar',type=Path,help='Override for reproducing a previous release')
parser.add_argument('--property',action='append',default=[],help='Additional test JVM property, key=value')
parser.add_argument('--expect-input-failure',action='store_true',help='Negative control: require the previous direct org.lwjglx input failure')
args=parser.parse_args()
side=args.side
name=args.name
if not name or any(c not in 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_' for c in name):parser.error('Use a simple scenario name')
run=root/'.dev/ae2-addons-verification'/name/side
evidence=project/'verification/1.0.2'/name
if run.exists() or evidence.exists():parser.error('Use a fresh --name')
evidence.mkdir(parents=True)
version=re.search(r'^mod_version\s*=\s*(\S+)',(project/'gradle.properties').read_text(),re.MULTILINE).group(1)
mods=[root/'.dev/ae2s-1.1.0/AppliedEnergistics2-Supergiant-1.1.0.jar',args.mod_jar.resolve() if args.mod_jar else project/f'build/libs/AE2-Crystal-Science-Legacy-{version}.jar',project/'build/verification/ae2csl-runtime-regression.jar',root/'1.12.2/mcp-testbridge/build/libs/MCP-TestBridge-Cleanroom-1.12.2-0.1.0.jar']
if side=='server' and args.loader_version=='0.4.0':mods.append(root/'1.12.2/cleanroom-040-compat/build/libs/Cleanroom-0.4.0-Server-Compatibility-1.0.0.jar')
for extra in args.extra:mods.append(extra.resolve())
port=args.bridge_port
launcher=root/'tools/verify_ae2_addons.py' if args.loader_version=='0.4.0' else root/'1.12.2/MMCE-Complement/verification/launch.py'
command=[sys.executable,str(launcher),side,'--name',name,'--port','25891','--timeout','240','--report','acceptance.json','--property','mcp.testbridge.enabled=true','--property',f'mcp.testbridge.port={port}','--property',f'ae2csl.verification.cleanroom={args.loader_version}']
if args.loader_version!='0.4.0':command += ['--cleanroom-version',args.loader_version]
if side=='client':command += ['--property','mcp.testbridge.autoworld=true']
for prop in args.property:command += ['--property',prop]
for mod in mods:command += ['--mod',str(mod)]
result={'result':'FAIL','cleanroom':args.loader_version,'side':side}
bridge=None
with (evidence/'launcher.log').open('w') as log, (evidence/'bridge.jsonl').open('w') as transcript:
 process=subprocess.Popen(command,cwd=root,env=dict(os.environ,DISPLAY=args.display,LIBGL_ALWAYS_SOFTWARE='true'),stdout=log,stderr=subprocess.STDOUT)
 def call(tool,**args):
  data=bridge.call(tool,**args)
  transcript.write(json.dumps({'tool':tool,'arguments':args,'result':data},ensure_ascii=False)+'\n');transcript.flush()
  return data
 sequence=0
 def input_request(action,**arguments):
  global sequence
  sequence+=1
  request={'id':sequence,'action':action,**arguments}
  temporary=run/'ae2csl-input-request.tmp'
  temporary.write_text(json.dumps(request));temporary.replace(run/'ae2csl-input-request.json')
  deadline=time.monotonic()+20
  while time.monotonic()<deadline:
   response_file=run/'ae2csl-input-response.json'
   if response_file.exists():
    response=json.loads(response_file.read_text())
    if response.get('id')==sequence:
     transcript.write(json.dumps({'input':request,'result':response})+'\n');transcript.flush()
     assert response['result']=='PASS',response
     return response
   if process.poll() is not None:raise RuntimeError('Client exited during input test')
   time.sleep(.1)
  raise TimeoutError('No input response')
 try:
  deadline=time.monotonic()+205
  while time.monotonic()<deadline:
   if process.poll() is not None:raise RuntimeError('Game exited early; see console.log')
   if (run/'mcp-testbridge/endpoint.json').exists():
    bridge=bridge or Bridge(run/'mcp-testbridge')
    if (run/'ae2csl-runtime-regression.json').exists():
     report=json.loads((run/'ae2csl-runtime-regression.json').read_text())
     if report.get('result')=='FAIL':raise AssertionError(report)
     if report.get('result')=='PASS' and (side=='server' or report.get('clientWorldGuard')):
      result['regression']=report;break
   time.sleep(.5)
  else:raise TimeoutError('No completed regression report')
  result['mods']=call('game.status')['mods']
  assert result['mods']['ae2']=='1.1.0'
  if not args.expect_input_failure:assert result['mods']['ae2csl']==version
  if side=='client':
   for i in range(60):
    try:call('client.inspect');break
    except RuntimeError:time.sleep(.5)
   call('world.await_ticks',ticks=20)
   call('client.view',closeScreen=True)
   preflight=json.loads((run/'ae2csl-input-preflight.json').read_text())
   result['inputPreflight']=preflight
   if args.expect_input_failure:
    assert preflight['result']=='FAIL' and 'org.lwjglx.input.Keyboard' in preflight.get('error','') and 'MirrorLinkerListener.onKeyInput' in preflight['error'],preflight
    result['expectedFailureReproduced']=True
   else:
    assert preflight['result']=='PASS',preflight
    input_results=[]
    call('world.test_player',action='item',id='minecraft:air',count=0,slot=0,selectedSlot=0)
    call('world.await_ticks',ticks=3)
    input_results.append(input_request('key',hitBlock=True))
    call('world.test_player',action='item',id='ae2csl:mirror_linker',slot=0,selectedSlot=0,sneaking=False)
    call('world.await_ticks',ticks=5)
    input_results.append(input_request('key',hitBlock=True))
    input_results.append(input_request('pollMouse'))
    call('client.view',screen='inventory')
    input_results.append(input_request('key',hitBlock=True))
    assert not input_request('wheel',delta=-120)['canceled']
    call('client.view',closeScreen=True)
    call('world.test_player',action='item',id='ae2csl:resonating_memory_card',slot=0,selectedSlot=0,sneaking=False)
    call('world.await_ticks',ticks=5)
    assert not input_request('wheel',delta=-120)['canceled']
    # Drive the real sneak binding on real client ticks, then post a wheel event.
    call('client.key',key=42,down=True,holdTicks=120)
    call('world.await_ticks',ticks=5)
    before=input_request('inspect')
    wheel=input_request('wheel',delta=-120)
    assert wheel['canceled'],wheel
    call('world.await_ticks',ticks=8)
    after=input_request('inspect')
    assert after['hotbar']==before['hotbar']==0 and after['cardSlot']==(before['cardSlot']+1)%9,(before,after)
    back=input_request('wheel',delta=120)
    assert back['canceled'],back
    call('world.await_ticks',ticks=8)
    restored=input_request('inspect')
    assert restored['cardSlot']==before['cardSlot'] and restored['hotbar']==0,restored
    call('client.key',key=42,down=False)
    call('world.await_ticks',ticks=3)
    result['inputTests']={'keyEventsAndMousePolling':input_results,'cardBefore':before,'cardNext':after,'cardRestored':restored}
   ids=['ae2csl:resonating_pattern_provider_part','ae2csl:extended_resonating_pattern_provider_part','ae2csl:resonating_memory_card']
   result['models']=[]
   for item in ids:
    call('world.test_player',action='item',id=item,slot=0,selectedSlot=0)
    call('world.await_ticks',ticks=2)
    model=call('client.inspect_tool')
    assert not model.get('missing',True),model
    result['models'].append(model)
   call('world.set_block',x=3,y=4,z=3,id='ae2csl:resonating_pattern_provider')
   call('world.set_block',x=5,y=4,z=3,id='ae2:cable_bus')
   call('world.await_ticks',ticks=30)
   result['machine']=call('world.get_block',x=3,y=4,z=3)
   result['cableBus']=call('world.get_block',x=5,y=4,z=3)
   assert result['machine'].get('tile','').startswith('ae2csl.'),result['machine']
   result['client']=call('client.inspect')
   assert result['client']['glError']==0
   shot=Path(call('client.screenshot',name='ae2csl-fixed'))
   shutil.copy2(shot,evidence/'client.png')
  result['result']='PASS'
 except Exception:
  result['error']=traceback.format_exc()
 finally:
  (evidence/'report.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
  if run.exists():(run/'acceptance.json').write_text(json.dumps(result))
  if bridge and process.poll() is None:
   try:call('game.shutdown')
   except Exception:pass
  try:process.wait(timeout=30)
  except subprocess.TimeoutExpired:process.terminate();process.wait(timeout=20)
  for file in ['runtime.json','result.json','console.log','ae2csl-runtime-regression.json','ae2csl-input-preflight.json']:
   if (run/file).is_file():shutil.copy2(run/file,evidence/file)
print(json.dumps({'side':side,'result':result['result'],'exit':process.returncode,'evidence':str(evidence),'error':result.get('error')},ensure_ascii=False),flush=True)
raise SystemExit(0 if result['result']=='PASS' and process.returncode==0 else 1)
