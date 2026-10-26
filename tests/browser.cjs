// npm ci; npx playwright install chromium; npm test
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const path = require('node:path');
const fs = require('node:fs/promises');
(async () => {
  const html = await fs.readFile(path.resolve(__dirname,'../docs/index.html'));
  const server = require('node:http').createServer((req,res)=>{res.setHeader('Content-Type','text/html; charset=utf-8');res.end(html);});
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const browser = await chromium.launch({executablePath:process.env.CHROMIUM_PATH || (require('node:fs').existsSync('/usr/bin/chromium') ? '/usr/bin/chromium' : undefined),headless:true,args:['--no-sandbox']});
  try {
    const context = await browser.newContext({viewport:{width:390,height:844},acceptDownloads:true});
    const page = await context.newPage();
    const errors=[]; page.on('pageerror',e=>errors.push(e.message));
    await page.goto('http://127.0.0.1:'+server.address().port);
    await page.evaluate(()=>{localStorage.clear();}); await page.reload();
    await page.getByRole('button',{name:'サンプルを入力'}).click();
    assert.equal(await page.locator('#pain').inputValue(),'4'); assert.equal(await page.locator('#abdominalPain').inputValue(),'2');assert.equal(await page.locator('#headache').inputValue(),'7');
    assert.equal(await page.getByRole('checkbox').count(),14);
    assert.equal(await page.getByRole('checkbox',{name:'全身',exact:true}).count(),0);
    assert.deepEqual(await page.locator('#entry-form h2').allTextContents(),['記録日時','症状の強さ','痛む部位','腹痛・頭痛','気になる身体症状','メモ']);
    await page.getByRole('checkbox',{name:'腰',exact:true}).check();
    await page.screenshot({path:'/tmp/pain-preview-mobile.png',fullPage:true});
    await page.locator('#notes').fill('=SUM(1,2)\n"試験" <script>alert(1)</script>');
    await page.getByRole('button',{name:'この内容を保存'}).click();
    await page.locator('.entry').waitFor(); assert.equal(await page.locator('.entry').count(),1);
    assert.match(await page.locator('.entry').innerText(),/腹痛：2 \/ 10.*頭痛：7 \/ 10/);
    await page.reload(); await page.getByRole('tab',{name:'履歴・書き出し'}).click();
    assert.equal(await page.locator('.entry').count(),1);
    assert.match(await page.locator('.entry').innerText(),/痛む部位：肩、背中、腰/);
    await page.getByRole('button',{name:'編集',exact:true}).click(); await page.locator('#headache').selectOption('0');
    assert.ok(await page.getByRole('checkbox',{name:'肩',exact:true}).isChecked());
    assert.ok(await page.getByRole('checkbox',{name:'腰',exact:true}).isChecked());
    await page.getByRole('checkbox',{name:'肩',exact:true}).uncheck();
    await page.getByRole('checkbox',{name:'手・指',exact:true}).check();
    await page.getByRole('button',{name:'変更を保存'}).click();assert.equal(await page.locator('.entry').count(),1);assert.match(await page.locator('.entry').innerText(),/頭痛：なし/);
    const downloadEvent=page.waitForEvent('download');await page.getByRole('button',{name:'CSVに書き出す'}).click();const download=await downloadEvent;
    const csv=await fs.readFile(await download.path(),'utf8');assert.ok(csv.startsWith('\ufeff'));assert.ok(csv.includes('身体の痛み(1-10)'));assert.ok(csv.includes('痛む部位（選択）'));assert.ok(csv.includes('"背中、腰、手・指","左肩の外側"'));assert.ok(csv.includes('"2 / 10","なし"'));assert.ok(csv.includes('"\'=SUM(1,2)\n""試験"" <script>'));
    assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
    await page.getByRole('button',{name:'削除',exact:true}).click(); await page.getByRole('button',{name:'戻る',exact:true}).click();assert.equal(await page.locator('.entry').count(),1);
    await page.getByRole('button',{name:'削除',exact:true}).click(); await page.getByRole('button',{name:'続ける',exact:true}).click(); assert.equal(await page.locator('.entry').count(),0);
    await page.reload();await page.getByRole('tab',{name:'履歴・書き出し'}).click();assert.equal(await page.locator('.entry').count(),0);
    // A pre-checkbox browser record keeps its free text without guessing regions.
    await page.evaluate(()=>localStorage.setItem('pain-log-browser-v1',JSON.stringify([{id:'legacy',time:123,pain:2,fatigue:3,discomfort:4,abdominalPain:-1,headache:-1,symptoms:[-1,-1,-1,-1,-1,-1,-1],location:'左肩、右手の指先',notes:'旧記録'}])));
    await page.reload();await page.getByRole('tab',{name:'履歴・書き出し'}).click();
    assert.match(await page.locator('.entry').innerText(),/部位の補足：左肩、右手の指先/);
    await page.getByRole('button',{name:'編集',exact:true}).click();
    assert.equal(await page.locator('input[type=checkbox]:checked').count(),0);
    await page.getByRole('checkbox',{name:'肩',exact:true}).check();
    await page.getByRole('button',{name:'変更を保存'}).click();await page.reload();
    await page.getByRole('tab',{name:'履歴・書き出し'}).click();assert.match(await page.locator('.entry').innerText(),/痛む部位：肩/);
    // Retired whole-body selections survive editing without returning to the choices.
    await page.evaluate(()=>{const rows=JSON.parse(localStorage.getItem('pain-log-browser-v1'));rows[0].locationMask |= 1;localStorage.setItem('pain-log-browser-v1',JSON.stringify(rows));});
    await page.reload();await page.getByRole('tab',{name:'履歴・書き出し'}).click();
    await page.getByRole('button',{name:'編集',exact:true}).click();
    assert.equal(await page.getByRole('checkbox',{name:'全身',exact:true}).count(),0);
    await page.getByRole('button',{name:'変更を保存'}).click();
    assert.match(await page.locator('.entry').innerText(),/痛む部位：全身、肩/);
    assert.deepEqual(errors,[]);
    console.log('PASS: multi-select regions/save/reload/edit/legacy migration/CSV, independent pain values, safe CSV download, delete confirmation/persistence, mobile width, no script injection or JS errors');
    await context.close();
  } finally { await browser.close(); await new Promise(resolve=>server.close(resolve)); }
})().catch(e=>{console.error(e);process.exitCode=1;});
