# -*- coding: UTF-8 -*-
#!/usr/bin/python
import sqlite3
import pdfkit
from flask import Flask, request, session, g, redirect, url_for, \
     abort, render_template, flash,send_file
from flask import make_response
from contextlib import closing
import requests
import urllib
import random
import os
import sys
import yaml
import time
import json
requests_session = requests.session() 
basedir = os.path.abspath(os.path.dirname(__file__))
# create our little application :)
app = Flask(__name__)
app.config.from_object(__name__)
app.secret_key = '123456'
@app.route('/', methods=['GET', 'POST'])
def index():
    return render_template('index.html')
@app.route('/licenses', methods=['GET', 'POST'])
def licenses():
    conn = sqlite3.connect('data.db')
    netdisLi = []
    cursor = conn.execute("SELECT NAME  from NETDISTRIBUTIONLICENSE")
    for row in cursor:
        netdisLi.append(row[0])
    sameLi = []
    cursor = conn.execute("SELECT NAME  from sameLicense")
    for row in cursor:
        sameLi.append(row[0])
    openLi = []
    cursor = conn.execute("SELECT NAME  from OPENSOURCELICENSE")
    for row in cursor:
        openLi.append(row[0])
    notPatLi = []
    cursor = conn.execute("SELECT NAME  from NOTPATENTLICENSE")
    for row in cursor:
        notPatLi.append(row[0])
    ModLi = []
    cursor = conn.execute("SELECT NAME  from ModIFYLICENSE")
    for row in cursor:
        ModLi.append(row[0])
    TMLi = []
    cursor = conn.execute("SELECT NAME  from TRADEMARKLICENSE")
    for row in cursor:
        TMLi.append(row[0])
    licensenslist = []
    cursor = conn.execute("SELECT NAME,OWER,URL from LICENSESLIST")
    for row in cursor:
        licensenslist.append({'name':row[0],'owner':row[1],'url':row[2],'isNet':(row[0] in netdisLi),'isTM':(row[0] in TMLi),'isSame':(row[0] in sameLi),'isOS':(row[0] in openLi),'isNP':(row[0] in notPatLi),'isMo':(row[0] in ModLi)})
    return '{"results":'+json.dumps(licensenslist,encoding="utf-8")+'}'
@app.route('/addlicense', methods=['GET', 'POST'])
def addlicense():
    conn = sqlite3.connect('data.db')
    if(request.form['name']):
        name = request.form['name']
        licensefile = open("src/licensedcode/data/licenses/"+name+'.LICENSE', "w+")
	ymlfile = open("src/licensedcode/data/licenses/"+name+'.yml', "w+")
	ymlfile.write('key: '+name+'\n')
    if(request.form['fullname']):
        fullname = request.form['fullname']
        ymlfile.write('name: '+fullname+'\n')
    if(request.form['owner']):
        owner = request.form['owner']
        ymlfile.write('owner: '+owner+'\n')
    if(request.form['ownerurl']):
        ownerurl = request.form['ownerurl']
        ymlfile.write('homepage_url: '+ownerurl+'\n')
        ymlfile.write('text_urls: '+ownerurl+'\n')
        ymlfile.close()
        conn.execute("INSERT INTO LICENSESLIST (NAME,OWER,URL) VALUES (?,?,?)",(name,owner,ownerurl))
        conn.commit()
    if(request.form.getlist('atts')):
        atts = request.form.getlist('atts')
        if('0' in atts):
            conn.execute("INSERT INTO OPENSOURCELICENSE (NAME) VALUES (?)",(name,))
            conn.commit()
        if('1' in atts):
            conn.execute("INSERT INTO NETDISTRIBUTIONLICENSE (NAME) VALUES (?)",(name,))
            conn.commit()
        if('2' in atts):
            conn.execute("INSERT INTO sameLicense (NAME) VALUES (?)",(name,))
            conn.commit()
        if('3' in atts):
            conn.execute("INSERT INTO NOTPATENTLICENSE (NAME) VALUES (?)",(name,))
            conn.commit()
        if('4' in atts):
            conn.execute("INSERT INTO TRADEMARKLICENSE (NAME) VALUES (?)",(name,))
            conn.commit()
        if('5' in atts):
            conn.execute("INSERT INTO ModIFYLICENSE (NAME) VALUES (?)",(name,))
            conn.commit()
    if(request.form['textall']):
        textall = request.form['textall']
        licensefile.write(textall+'\n')
        licensefile.close()
    conn.close()
    return '添加成功!'
@app.route('/test', methods=['GET', 'POST'])
def test():
    if(request.form['giturl']):
        url = request.form['giturl']
        filename = url.rsplit('/')[-1].rsplit('.')[0]
        session['filename'] = filename
        cm = 'git clone '+request.form['giturl']+' templates/'+filename
        os.popen(cm)
        if(os.path.exists('templates/'+filename)):
            return redirect('/add')
        else:
            return '仓库git下载失败，请重试！'
    elif(request.files['file1']):
        file_dir=os.path.join(basedir,'templates')
        f=request.files['file1']
        filename = f.filename.lower()
        session['filename'] = filename.rsplit('.',1)[0]
        print filename
        f.save(os.path.join(file_dir,filename))
        if '.zip' in filename:
            cm = 'unzip -n '+file_dir+'/'+filename+' -d templates/'+session['filename']
            os.popen(cm)
            cm = 'rm -f '+file_dir+'/'+filename
            os.popen(cm)
        if '.tar.gz' in filename:
            session['filename'] = session['filename'].rsplit('.',1)[0]
            cm = "mkdir templates/"+session['filename']
            os.popen(cm)
            cm = 'tar -zxvf '+file_dir+'/'+filename+' -C templates/'+session['filename']
            os.popen(cm)
            cm = 'rm -f '+file_dir+'/'+filename
            os.popen(cm)
        if '.tar.bz2' in filename:
            session['filename'] = session['filename'].rsplit('.',1)[0]
            cm = "mkdir templates/"+session['filename']
            os.popen(cm)
            cm = 'tar -jcvf '+file_dir+'/'+filename+' -C templates/'+session['filename']
            os.popen(cm)
            cm = 'rm -f '+file_dir+'/'+filename
            os.popen(cm)
        if '.tar.xz' in filename:
            session['filename'] = session['filename'].rsplit('.',1)[0]
            cm = "mkdir templates/"+session['filename']
            os.popen(cm)
            cm = 'tar xvJf '+file_dir+'/'+filename+' -C templates/'+session['filename']
            os.popen(cm)
            cm = 'rm -f '+file_dir+'/'+filename
            os.popen(cm)
        if(os.path.exists('templates/'+session['filename'])):
            return redirect('/add')
        else:
            return '文件上传解压缩失败，请重试！'
    return '请重试！'
@app.route('/add', methods=['GET','POST'])
def add():
    name = session['filename']
    ISOTIMEFORMAT='-%Y-%m-%d-%X'
    scantime = time.strftime(ISOTIMEFORMAT,time.localtime())
    cm = './scancode -cl  -n 8 --format html templates/'+name+' templates/'+name+scantime+'.html'
    os.popen(cm)
    cm = 'rm -rf templates/'+name
    os.popen(cm)
    session['savename'] = name+scantime+'.html'
    return render_template(name+scantime+'.html')
@app.route('/src/licensedcode/data/licenses/<path>', methods=['GET','POST'])
def today(path):
    base_dir = os.path.dirname(__file__)+'src/licensedcode/data/licenses/'
    resp = open(os.path.join(base_dir, path)).read()#.replace('\n','<br />')
    #resp.headers["Content-type"]="text/html;charset=UTF-8"
    #return resp
    return render_template('licenses.html',data=resp)
@app.route('/savepdf', methods=['GET','POST'])
def savepfd():
    savename = 'templates/' + session['savename']
    pdfname = savename.rsplit('.',1)[0]+'.pdf'
    pdfkit.from_file(savename, pdfname)
    response = make_response(send_file(pdfname))
    response.headers["Content-Disposition"] = "attachment; filename="+pdfname.rsplit('.',1)[0].rsplit('/',1)[1]+".pdf;"
    return response
if __name__ == '__main__':
    if sys.getdefaultencoding() != 'utf8': 
        reload(sys)
        sys.setdefaultencoding('utf8')
    app.run(host='0.0.0.0',debug=True,threaded = True)

