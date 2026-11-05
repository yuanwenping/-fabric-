"""Visio drawing helpers: build simple B/W UML-style figures, save .vsdx and export .png."""
import os, glob, time, functools
sp = r'C:/Users/lx/.workbuddy/binaries/python/envs/default/Lib/site-packages'
for p in glob.glob(sp + '/pywin32_system32'):
    os.add_dll_directory(p)
import win32com.client as w


def retry(times=8, delay=1.2):
    def deco(fn):
        @functools.wraps(fn)
        def wrapper(*a, **kw):
            last = None
            for i in range(times):
                try:
                    return fn(*a, **kw)
                except Exception as e:
                    last = e
                    time.sleep(delay)
            raise last
        return wrapper
    return deco

SCALE = 3.0            # draw at 3x so exported png is ~330 dpi at design size
LW = 0.6               # design line weight (pt)
MAIN_FONT = '宋体'
LATIN_FONT = 'Times New Roman'
BLACK = 'RGB(0,0,0)'
WHITE = 'RGB(255,255,255)'

_app = None


def app():
    global _app
    if _app is None:
        _app = w.gencache.EnsureDispatch('Visio.Application')
        try:
            _app.Visible = 0
            _app.ScreenUpdating = 0
            _app.AlertResponse = 1
            _app.DeferScreenUpdates = True
        except Exception:
            pass
        time.sleep(0.5)
    return _app


class Fig:
    def __init__(self, w, h, title='fig', ox=0.0):
        self.w, self.h, self.title = w, h, title
        self.ox = ox
        self.doc = self._new_doc()
        self.pg = self.doc.Pages.Item(1)
        ps = self.pg.PageSheet
        try:
            ps.CellsU('DrawingSizeType').FormulaU = '3'   # custom page size, no auto-fit
        except Exception:
            pass
        ps.CellsU('PageWidth').FormulaU = f'{w*SCALE} in'
        ps.CellsU('PageHeight').FormulaU = f'{h*SCALE} in'
        for m in ('PageLeftMargin', 'PageRightMargin', 'PageTopMargin', 'PageBottomMargin'):
            try:
                ps.CellsU(m).FormulaU = '0 in'
            except Exception:
                pass

    @retry()
    def _new_doc(self):
        return app().Documents.Add('')

    # ---------- primitives ----------
    def _Y(self, y):
        return (self.h - y) * SCALE

    def _style(self, s, text='', size=10.5, bold=False, align=1, valign=1,
               dash=False, textdir=0, fill=True, color=BLACK, italic=False):
        if text != '':
            s.Text = text
        s.CellsU('Char.Size').FormulaU = f'{size*SCALE} pt'
        s.CellsU('Char.Font').FormulaU = f'"{LATIN_FONT}"'
        s.CellsU('Char.AsianFont').FormulaU = f'"{MAIN_FONT}"'
        s.CellsU('Char.Color').FormulaU = color
        s.CellsU('Char.Style').FormulaU = str((1 if bold else 0) + (2 if italic else 0))
        s.CellsU('Para.HorzAlign').FormulaU = str(align)
        s.CellsU('VerticalAlign').FormulaU = str(valign)
        for m in ('LeftMargin', 'RightMargin'):
            s.CellsU(m).FormulaU = f'{0.02*SCALE} in'
        for m in ('TopMargin', 'BottomMargin'):
            s.CellsU(m).FormulaU = f'{0.01*SCALE} in'
        s.CellsU('LineColor').FormulaU = color
        s.CellsU('LineWeight').FormulaU = f'{LW*SCALE} pt'
        if dash:
            s.CellsU('LinePattern').FormulaU = '2'
        if textdir:
            s.CellsU('TextDirection').FormulaU = str(textdir)
        if fill:
            s.CellsU('FillForegnd').FormulaU = WHITE
            s.CellsU('FillPattern').FormulaU = '1'
        else:
            s.CellsU('FillPattern').FormulaU = '0'
        return s

    def rect(self, x, y, w, h, text='', **kw):
        x = x + self.ox
        s = self.pg.DrawRectangle(x*SCALE, self._Y(y+h), (x+w)*SCALE, self._Y(y))
        return self._style(s, text, **kw)

    def rrect(self, x, y, w, h, text='', r=0.08, **kw):
        s = self.rect(x, y, w, h, text, **kw)
        s.CellsU('Rounding').FormulaU = f'{r*SCALE} in'
        return s

    def ellipse(self, x, y, w, h, text='', **kw):
        x = x + self.ox
        s = self.pg.DrawOval(x*SCALE, self._Y(y+h), (x+w)*SCALE, self._Y(y))
        return self._style(s, text, **kw)

    def diamond(self, x, y, w, h, text='', **kw):
        cx, cy = x + w/2.0, y + h/2.0
        pts = [(cx, y), (x+w, cy), (cx, y+h), (x, cy), (cx, y)]
        s = self._poly(pts)
        return self._style(s, text, **kw)

    def _poly(self, pts, closed=True):
        arr = []
        for (x, y) in pts:
            arr.append((x + self.ox)*SCALE)
            arr.append(self._Y(y))
        return self.pg.DrawPolyline(arr, 0)

    def label(self, x, y, w, h, text, size=10.5, align=1, valign=1, bold=False):
        s = self.rect(x, y, w, h, text, size=size, align=align, valign=valign,
                      bold=bold, fill=False)
        s.CellsU('LinePattern').FormulaU = '0'
        return s

    def tag(self, x, y, w, h, text, size=8.5, align=1):
        s = self.rect(x, y, w, h, text, size=size, align=align, fill=True)
        s.CellsU('LinePattern').FormulaU = '0'
        return s

    def path(self, pts, arrow=True, both=False, dash=False, weight=None):
        s = self._poly(pts)
        s.CellsU('LineColor').FormulaU = BLACK
        s.CellsU('LineWeight').FormulaU = f'{(weight or LW)*SCALE} pt'
        if arrow:
            s.CellsU('EndArrow').FormulaU = '4'
            s.CellsU('EndArrowSize').FormulaU = '2'
        if both:
            s.CellsU('BeginArrow').FormulaU = '4'
            s.CellsU('BeginArrowSize').FormulaU = '2'
        if dash:
            s.CellsU('LinePattern').FormulaU = '2'
        s.CellsU('FillPattern').FormulaU = '0'
        return s

    def arrow(self, x1, y1, x2, y2, **kw):
        return self.path([(x1, y1), (x2, y2)], **kw)

    # ---------- composite ----------
    def actor(self, x, y, text, size=10.5, label_w=1.0):
        """x,y = top-left of head bounding box; figure ~1.05in tall."""
        r = 0.13
        hx = x + 0.18
        head = self.pg.DrawOval((hx + self.ox)*SCALE, self._Y(y + 2*r), (hx + self.ox + 2*r)*SCALE, self._Y(y))
        head.CellsU('LineColor').FormulaU = BLACK
        head.CellsU('LineWeight').FormulaU = f'{LW*SCALE} pt'
        head.CellsU('FillPattern').FormulaU = '0'
        cx = hx + r
        top = y + 2*r
        # body
        self.arrow(cx, top, cx, top + 0.34, arrow=False)
        # arms
        self.arrow(cx - 0.2, top + 0.09, cx + 0.2, top + 0.09, arrow=False)
        # legs
        self.arrow(cx, top + 0.34, cx - 0.18, top + 0.62, arrow=False)
        self.arrow(cx, top + 0.34, cx + 0.18, top + 0.62, arrow=False)
        self.label(cx - label_w/2.0, top + 0.64, label_w, 0.26, text, size=size)
        return (cx, top + 0.18)

    def boundary(self, x, y, w, h, title, size=10.5):
        self.rect(x, y, w, h, '', fill=False)
        self.label(x, y + 0.03, w, 0.24, title, size=size, bold=False)
        return (x, y, w, h)

    def save(self, vsdx_path, png_path):
        try:
            self.pg.Export(png_path)
            self._check(png_path)
        except Exception as e:
            print('  export fail', self.title, e)
        try:
            self.doc.SaveAs(vsdx_path)
        except Exception as e:
            print('  saveas fail', self.title, e)
        try:
            self.doc.Saved = True
        except Exception:
            pass


    def _check(self, png_path):
        try:
            from PIL import Image, ImageChops
            im = Image.open(png_path).convert('L')
            w, h = im.size
            bbox = ImageChops.difference(im, Image.new('L', (w, h), 255)).getbbox()
            if not bbox:
                print('  WARN blank', self.title); return
            if bbox[0] <= 1 or bbox[1] <= 1 or bbox[2] >= w - 1 or bbox[3] >= h - 1:
                print(f'  WARN overflow {self.title} bbox={bbox} size={(w, h)}')
        except Exception:
            pass


def quit_app():
    global _app
    if _app is not None:
        for i in range(1, _app.Documents.Count + 1):
            try:
                _app.Documents.Item(i).Saved = True
            except Exception:
                pass
        try:
            _app.Quit()
        except Exception:
            pass
        _app = None
