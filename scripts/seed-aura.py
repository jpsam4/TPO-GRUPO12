"""
Carga el grafo semilla en Neo4j Aura usando la Query API sobre HTTPS (puerto 443).

Existe porque la red de la facultad bloquea TLS sobre el puerto 7687 (Bolt),
asi que el driver de Spring no puede llegar a Aura desde ahi. La Query API va
por 443 y no la bloquea nadie, asi que sirve para dejar la base de la nube
cargada y para verificarla.

Escribe exactamente lo mismo que CargaInicial.java, con el mismo mapeo que usa
Spring Data Neo4j: (:Nodo {id, nombre, tipo, valor}), (:Item {id, nombre, peso,
valor, nodoId}) y relaciones [:CONECTA {costo}].

Uso:  python scripts/seed-aura.py [--reset] [--vaciar] [--verificar]
       --reset      vacia y vuelve a cargar la semilla
       --vaciar     solo vacia (sirve para comprobar que la app carga sola)
       --verificar  cuenta nodos, aristas e items y controla que sean 8/12/5
Lee las credenciales de .env (no las trae hardcodeadas).
"""
import json, os, sys, urllib.request, base64, pathlib

RAIZ = pathlib.Path(__file__).resolve().parent.parent

def cargar_env():
    env = {}
    ruta = RAIZ / ".env"
    if not ruta.exists():
        sys.exit("Falta el archivo .env. Copiar .env.example y completarlo.")
    for linea in ruta.read_text(encoding="utf-8").splitlines():
        linea = linea.strip()
        if linea and not linea.startswith("#") and "=" in linea:
            k, v = linea.split("=", 1)
            env[k.strip()] = v.strip()
    return env

ENV  = cargar_env()
HOST = ENV["NEO4J_URI"].split("://", 1)[1].split(":")[0]
BASE = ENV.get("NEO4J_DATABASE", "neo4j")
AUTH = base64.b64encode(f'{ENV["NEO4J_USER"]}:{ENV["NEO4J_PASSWORD"]}'.encode()).decode()
URL  = f"https://{HOST}/db/{BASE}/query/v2"

def cypher(statement, parameters=None):
    cuerpo = json.dumps({"statement": statement, "parameters": parameters or {}}).encode()
    pedido = urllib.request.Request(URL, data=cuerpo, headers={
        "Authorization": f"Basic {AUTH}",
        "Content-Type": "application/json",
        "Accept": "application/json",
    })
    with urllib.request.urlopen(pedido, timeout=60) as r:
        rta = json.load(r)
    if "errors" in rta:
        sys.exit(f"Error de Neo4j: {rta['errors']}")
    return rta["data"]["values"]

NODOS = [
    ("A", "Nodo A", "CENTRO",  0), ("B", "Nodo B", "PUNTO", 12),
    ("C", "Nodo C", "PUNTO",   8), ("D", "Nodo D", "PUNTO", 15),
    ("E", "Nodo E", "PUNTO",   6), ("F", "Nodo F", "PUNTO", 20),
    ("G", "Nodo G", "PUNTO",   9), ("H", "Nodo H", "PUNTO",  4),
]
# El directo A->C cuesta 9, pero A->B->C cuesta 7: sirve para ver la relajacion.
ARISTAS = [("A","B",4),("A","C",9),("B","C",3),("B","D",7),("C","D",2),("C","E",11),
           ("D","E",5),("D","F",8),("E","F",6),("E","G",10),("F","H",3),("G","H",4)]
# Con capacidad 10 el greedy por ratio elige {I4,I2}=90 y el optimo es {I4,I5}=105.
ITEMS = [("I1","Item 1",5,10,"A"), ("I2","Item 2",4,40,"B"), ("I3","Item 3",6,30,"C"),
         ("I4","Item 4",3,50,"D"), ("I5","Item 5",7,55,"E")]

def resumen():
    v = cypher("""
        MATCH (n:Nodo) WITH count(n) AS nodos
        MATCH ()-[r:CONECTA]->() WITH nodos, count(r) AS aristas
        MATCH (i:Item) RETURN nodos, aristas, count(i) AS items""")
    return tuple(v[0]) if v else (0, 0, 0)

def verificar():
    n, a, i = resumen()
    print(f"Aura {HOST} base '{BASE}': {n} nodos, {a} aristas, {i} items")
    ok = (n, a, i) == (8, 12, 5)
    print("OK: coincide con la semilla esperada" if ok else "ATENCION: no coincide con 8/12/5")
    return 0 if ok else 1

def main():
    # El orden importa: --reset se aplica ANTES que --verificar, para que
    # "--reset --verificar" haga lo que se espera (vaciar, cargar y controlar)
    # en vez de verificar lo viejo y salir.
    if "--reset" in sys.argv or "--vaciar" in sys.argv:
        cypher("MATCH (n) DETACH DELETE n")
        print("Base vaciada.")

    # --vaciar deja la base vacia a proposito y no carga nada: sirve para
    # comprobar que la aplicacion es la que escribe en la nube al arrancar.
    if "--vaciar" in sys.argv and "--reset" not in sys.argv:
        print("Aura quedo en:", resumen())
        return 0

    # --verificar solo (sin --reset) no carga nada: solo mira.
    if "--verificar" in sys.argv and "--reset" not in sys.argv:
        return verificar()

    n, _, _ = resumen()
    if n > 0:
        print("La base ya tiene datos: no se vuelve a cargar (usar --reset para forzar).")
        return verificar() if "--verificar" in sys.argv else 0

    cypher("UNWIND $filas AS f CREATE (:Nodo {id:f[0], nombre:f[1], tipo:f[2], valor:toFloat(f[3])})",
           {"filas": NODOS})
    cypher("""UNWIND $filas AS f
              MATCH (o:Nodo {id:f[0]}), (d:Nodo {id:f[1]})
              CREATE (o)-[:CONECTA {costo: toFloat(f[2])}]->(d)""", {"filas": ARISTAS})
    cypher("""UNWIND $filas AS f
              CREATE (:Item {id:f[0], nombre:f[1], peso:toFloat(f[2]),
                             valor:toFloat(f[3]), nodoId:f[4]})""", {"filas": ITEMS})

    n, a, i = resumen()
    print(f"Carga completada en Aura: {n} nodos, {a} aristas, {i} items.")
    return verificar() if "--verificar" in sys.argv else 0

if __name__ == "__main__":
    sys.exit(main())
