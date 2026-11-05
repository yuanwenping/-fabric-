import os, glob, sys
sp = r'C:/Users/lx/.workbuddy/binaries/python/envs/default/Lib/site-packages'
for p in glob.glob(sp + '/pywin32_system32'):
    os.add_dll_directory(p)
import win32com.client as w
from PIL import Image

BASE = r'E:\毕业项目实训\pakeage\docs\图源文件'

app = w.gencache.EnsureDispatch('Visio.Application')
doc = app.Documents.Add('')
pg = doc.Pages.Item(1)
S = 3.0
pg.PageSheet.CellsU('PageWidth').FormulaU = f'{5*S} in'
pg.PageSheet.CellsU('PageHeight').FormulaU = f'{3.2*S} in'

def Y(y):  # top-left origin -> visio
    return (3.2 - y) * S

def rect(x, y, w, h, text='', size=10.5, rounding=None, dash=False, textdir=0, vert=1):
    s = pg.DrawRectangle(x*S, Y(y+h), (x+w)*S, Y(y))
    s.Text = text
    s.CellsU('Char.Size').FormulaU = f'{size*S} pt'
    s.CellsU('Char.Font').FormulaU = '"宋体"'
    s.CellsU('Char.AsianFont').FormulaU = '"宋体"'
    s.CellsU('Para.HorzAlign').FormulaU = '1'
    s.CellsU('VerticalAlign').FormulaU = str(vert)
    s.CellsU('Char.Color').FormulaU = 'RGB(0,0,0)'
    if rounding:
        s.CellsU('Rounding').FormulaU = f'{rounding} in'
    if dash:
        s.CellsU('LinePattern').FormulaU = '2'
    if textdir:
        s.CellsU('TextDirection').FormulaU = str(textdir)
    return s

def path(pts, arrow=True, dash=False):
    arr = []
    for (x, y) in pts:
        arr.append(x*S); arr.append(Y(y))
    s = pg.DrawPolyline(arr, 0)
    s.CellsU('LineColor').FormulaU = 'RGB(0,0,0)'
    s.CellsU('LineWeight').FormulaU = f'{0.75*S} pt'
    if arrow:
        s.CellsU('EndArrow').FormulaU = '4'
        s.CellsU('EndArrowSize').FormulaU = '2'
    if dash:
        s.CellsU('LinePattern').FormulaU = '2'
    return s

def diamond(x, y, w, h, text=''):
    cx, cy = x + w/2, y + h/2
    return path([(cx, y), (x+w, cy), (cx, y+h), (x, cy), (cx, y)], arrow=False)

# sample: start(rrect) -> decision(diamond) -> two branches
rect(1.2, 0.2, 1.6, 0.5, '开始', rounding=0.15)
path([(2.0, 0.7), (2.0, 1.05)])
d = pg.DrawPolyline([1.1*S, Y(1.55), 2.0*S, Y(1.05), 2.9*S, Y(1.55), 2.0*S, Y(2.05), 1.1*S, Y(1.55)], 0)
d.Text = '信息匹配?'
d.CellsU('Char.Size').FormulaU = f'{10.5*S} pt'
d.CellsU('Char.Font').FormulaU = '"宋体"'
d.CellsU('Para.HorzAlign').FormulaU = '1'
path([(2.9, 1.55), (3.6, 1.55), (3.6, 2.4)])
rect(3.0, 2.4, 1.2, 0.5, '登录成功')
t = rect(3.65, 1.4, 0.35, 0.28, '否', size=10.5)
t.CellsU('LinePattern').FormulaU = '0'
t.CellsU('FillPattern').FormulaU = '0'
vt = rect(0.3, 1.2, 0.6, 1.2, '业务层', size=10.5, textdir=1)

out = os.path.join(BASE, 't2.png')
pg.Export(out)
print('png', Image.open(out).size)
app.Quit()
