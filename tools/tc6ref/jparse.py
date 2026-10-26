"""Tiny Java expression parser for TC6 ConfigRecipes statements."""
import re
TOK=re.compile(r'\s*(?:(?P<str>"(?:\\.|[^"\\])*")|(?P<chr>\'(?:\\.|[^\'\\])\')|(?P<num>-?\d+(?:\.\d+)?[fFL]?)|(?P<id>[A-Za-z_][\w]*)|(?P<op>\(|\)|\{|\}|\[|\]|,|\.|;|=|\+|\-|\*|/|<|>|!|\?|:|&&|\|\|))')
def tokens(s):
    i=0;out=[]
    while i<len(s):
        m=TOK.match(s,i)
        if not m:
            if s[i:].strip()=='' :break
            raise SyntaxError(s[i:i+40])
        i=m.end()
        for k,v in m.groupdict().items():
            if v is not None: out.append((k,v));break
    return out
class P:
    def __init__(s,toks): s.t=toks;s.i=0
    def peek(s,o=0): return s.t[s.i+o] if s.i+o<len(s.t) else (None,None)
    def eat(s,v=None):
        t=s.t[s.i]
        if v and t[1]!=v: raise SyntaxError(f'expected {v} got {t} at {s.t[s.i-5:s.i+5]}')
        s.i+=1;return t
    def args(s):
        s.eat('(');a=[]
        while s.peek()[1]!=')':
            a.append(s.expr())
            if s.peek()[1]==',':s.eat(',')
        s.eat(')');return a
    def expr(s):
        e=s.primary()
        while s.peek()[1] in ('+','-','*','/'):
            op=s.eat()[1];r=s.primary();e=('bin',op,e,r)
        return e
    def primary(s):
        k,v=s.peek()
        if k=='str': s.eat();return ('str',bytes(v[1:-1],'utf-8').decode('unicode_escape'))
        if k=='chr': s.eat();return ('chr',v[1])
        if k=='num': s.eat();return ('num',float(v.rstrip('fFL')) if '.' in v else int(v.rstrip('fFL')))
        if v=='(':
            s.eat('(');
            # cast?
            if s.peek()[0]=='id' and s.peek(1)[1]==')' and s.peek()[1][0].isupper():
                s.eat();s.eat(')');return s.primary()
            e=s.expr();s.eat(')');return s.post(e)
        if v=='new':
            s.eat();name=s.qual()
            while s.peek()[1]=='<':
                d=0
                while True:
                    t=s.eat()[1]
                    if t=='<':d+=1
                    if t=='>':d-=1
                    if d==0:break
            if s.peek()[1]=='[':
                s.eat('[');
                if s.peek()[1]!=']': s.expr()
                s.eat(']')
                while s.peek()[1]=='[': s.eat('[');s.eat(']')
                if s.peek()[1]=='{': return s.post(('array',name,s.arr()))
                return ('array',name,[])
            return s.post(('new',name,s.args()))
        if v=='{': return ('array',None,s.arr())
        if v=='!': s.eat();return ('not',s.primary())
        if v=='-': s.eat();return ('neg',s.primary())
        if k=='id':
            s.eat();e=('name',v)
            if s.peek()[1]=='(': e=('call',None,v,s.args())
            return s.post(e)
        raise SyntaxError(f'{k} {v} {s.t[s.i:s.i+8]}')
    def arr(s):
        s.eat('{');a=[]
        while s.peek()[1]!='}':
            a.append(s.expr())
            if s.peek()[1]==',':s.eat(',')
        s.eat('}');return a
    def qual(s):
        n=s.eat()[1]
        while s.peek()[1]=='.' and s.peek(1)[0]=='id' : s.eat('.');n+='.'+s.eat()[1]
        return n
    def post(s,e):
        while True:
            if s.peek()[1]=='.':
                s.eat('.');n=s.eat()[1]
                if s.peek()[1]=='(': e=('call',e,n,s.args())
                else: e=('field',e,n)
            elif s.peek()[1]=='[': s.eat('[');ix=s.expr();s.eat(']');e=('index',e,ix)
            else: return e
def statements(src):
    """Split method body into statements at top-level ';'."""
    out=[];d=0;cur=''
    in_s=False
    i=0
    while i<len(src):
        c=src[i]
        if c=='"' and src[i-1]!='\\': in_s=not in_s
        if not in_s:
            if c in '({':d+=1
            if c in ')}':d-=1
            if c==';' and d==0: out.append(cur.strip());cur='';i+=1;continue
            if c=='}' and d==0 and cur.lstrip().startswith(('for','if','while')): out.append((cur+c).strip());cur='';i+=1;continue
        cur+=c;i+=1
    return out
