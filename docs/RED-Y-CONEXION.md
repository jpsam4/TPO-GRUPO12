# Conexion a Neo4j: que funciona y que no desde la facultad

Documenta el diagnostico hecho el 19/08/2026, para no volver a investigarlo.

## El sintoma

La aplicacion arranca y se muere enseguida:

```
BoltServiceUnavailableException: Unable to write Bolt handshake to
3ae8699b.databases.neo4j.io(34.121.155.65):7687
Caused by: io.netty.handler.ssl.SslClosedEngineException: SSLEngine closed already
```

Parece un problema de credenciales o que la instancia de Aura esta caida.
**No es ninguna de las dos cosas.**

## El diagnostico

La red de UADE permite TLS **solo en los puertos conocidos**. En cualquier otro
puerto deja abrir el TCP y manda un RST apenas ve el ClientHello. Medido con
`scripts/probe-red.py`:

| Destino | Puerto | TCP | TLS |
|---|---|---|---|
| Aura | 443 | abre | **OK** |
| Gmail IMAPS | 993 | abre | **OK** |
| Gmail SMTPS | 465 | abre | **OK** |
| Aura Bolt | 7687 | abre | RST |
| Neo4j demo (otro host) | 7687 | abre | RST |
| Cloudflare | 2053 / 2083 / 2087 / 2096 / 8443 | abre | RST |
| 1.1.1.1 DNS-over-TLS | 853 | abre | RST |

Cloudflare sirve HTTPS de verdad en 2053 y 8443, asi que el RST no es del
servidor: es del firewall. **No hay ningun puerto alto por donde sacar TLS.**

Conclusiones:

- No es un problema de Aura ni de las credenciales.
- No sirve buscar "otro puerto": no hay.
- El TCP abre igual, asi que un test que solo abra el socket da falso positivo.
  Hay que probar el handshake completo (es lo que hace `TransporteNeo4j`).

## La solucion

Neo4j ofrece **dos protocolos** para lo mismo:

| Protocolo | Puerto | En la facultad |
|---|---|---|
| Bolt | 7687 | bloqueado |
| Query API (HTTPS) | 443 | **funciona** |

La aplicacion usa el que este disponible. Al arrancar, `TransporteNeo4j` prueba
Bolt de verdad (TCP + TLS + handshake + respuesta del servidor) y activa el
perfil que corresponda:

- **`bolt`** → Spring Data Neo4j sobre Bolt. Es el camino normal: fuera de la
  facultad, contra Neo4j local y en Render.
- **`http`** → `AlmacenHttp`, que habla Cypher por la Query API sobre HTTPS.

Los dos leen y escriben el mismo modelo, asi que se ven los mismos datos. No
hay que configurar nada: se elige solo. Para forzarlo, `NEO4J_TRANSPORTE=bolt`
o `=http` en el `.env`.

> Esto no evade el firewall: usa el otro protocolo oficial que la base ya
> ofrece, por el puerto que la red habilita.

## La otra trampa: variables de entorno viejas

Esta maquina tenia variables de usuario de Windows (`HKCU\Environment`) de una
epoca en que se trabajaba con Neo4j local:

```
NEO4J_URI=bolt://localhost:7687
NEO4J_USER=neo4j
NEO4J_PASSWORD=tpogrupo12
```

Spring ordena la configuracion asi:

```
propiedades del sistema  >  variables de entorno  >  archivos (.env incluido)
```

o sea que **esas variables le ganaban al `.env`** y la aplicacion se conectaba a
localhost sin decir nada. Por eso `TransporteNeo4j` sube lo del `.env` a
propiedades del sistema antes de arrancar Spring, y avisa por consola cuando el
ambiente dice otra cosa:

```
[Neo4j] AVISO: la variable de entorno NEO4J_URI dice algo distinto que el .env.
        Gana el .env. (ambiente=bolt://localhost:7687)
```

Si aparece ese aviso, conviene borrar las variables viejas:

```powershell
[Environment]::SetEnvironmentVariable('NEO4J_URI',      $null, 'User')
[Environment]::SetEnvironmentVariable('NEO4J_USER',     $null, 'User')
[Environment]::SetEnvironmentVariable('NEO4J_PASSWORD', $null, 'User')
```

(hay que abrir una consola nueva para que el cambio se note)

## Herramientas

```bash
python scripts/probe-red.py              # que puertos y protocolos deja pasar la red
python scripts/seed-aura.py --verificar  # cuenta nodos/aristas/items en Aura por HTTPS
python scripts/seed-aura.py --vaciar     # vacia la base (la app la recarga al arrancar)
python scripts/seed-aura.py --reset      # vacia y vuelve a cargar la semilla
```
