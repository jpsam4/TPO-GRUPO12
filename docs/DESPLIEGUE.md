# Despliegue y configuración del repositorio

Todo lo que hay que hacer **una sola vez** para que el equipo comparta la misma
aplicación y la misma base. Después de esto, cada push a `main` redespliega solo.

Las tres cosas de esta guía las tiene que hacer una persona con acceso a las
cuentas. No se pueden automatizar desde el repo porque implican meter
credenciales en paneles web.

---

## 1. Secrets del repositorio — **YA ESTA HECHO**

Los cuatro secrets estan cargados y el keep-alive se probo corriendo:

```
NEO4J_URI  NEO4J_USER  NEO4J_PASSWORD  NEO4J_DATABASE     (19/08/2026)

Actions > Mantener Aura viva > run 32263445860 -> success (7s)
Respuesta: {"data":{"fields":["nodos"],"values":[[8]]}, ...}
Aura respondio bien: la instancia sigue activa.
```

El workflow consulta Aura cada dos dias para que la instancia gratuita no se
pause y termine borrada, como ya paso con la instancia `8b37b6ab`. Los valores
quedan enmascarados en los logs (`https://***.databases.neo4j.io/db/***/...`).

Para correrlo a mano: **Actions → Mantener Aura viva → Run workflow**.

Si alguna vez hay que rotar la password de Aura, hay que actualizar **los dos
lados**: los secrets del repo y las variables del servicio en Render.

```bash
gh secret set NEO4J_PASSWORD     # la pide por consola, no queda en el historial
```

> **Ojo:** el usuario y la base de esta instancia **no** son `neo4j`, son el id
> de la instancia (`3ae8699b`). Es el error que mas tiempo hace perder.

---

## 2. Servicio en Render

`render.yaml` ya está en el repo, así que Render crea el servicio solo.

1. Entrar a [render.com](https://render.com) con la cuenta de GitHub.
2. **New → Blueprint**, elegir el repositorio `TPO-GRUPO12`.
3. Render lee `render.yaml` y propone el servicio `tpo-grupo12`.
4. Pide los cuatro valores marcados `sync: false`. Son **los mismos** de arriba:
   `NEO4J_URI`, `NEO4J_USER`, `NEO4J_PASSWORD`, `NEO4J_DATABASE`.
5. **Apply**. El primer build tarda bastante (compila con Maven dentro de Docker).

Cuando termina, verificar:

```bash
curl https://tpo-grupo12.onrender.com/api/grafo/resumen
# -> {"vertices":8,"aristas":12,...}
```

### Qué esperar

- **Cada push a `main` redespliega.** Es `autoDeploy: true` en `render.yaml`.
- **Se duerme a los ~15 minutos sin tráfico.** La siguiente visita tarda cerca
  de un minuto. Es el plan free, no es un bug.
- **Desde Render, Bolt funciona.** El bloqueo del puerto 7687 es de la red de la
  facultad, no de Render, así que allá la app elige sola el perfil `bolt` y usa
  Spring Data Neo4j. Es el único lugar donde ese camino se ejercita de verdad:
  si el log de arranque dice `Bolt responde`, esa parte está sana.

### Si el deploy falla

| En el log dice | Qué pasa |
|---|---|
| `DatabaseNotFound: Database name: 'neo4j'` | falta el env var `NEO4J_DATABASE` |
| `Unauthorized` / `Invalid credential` | `NEO4J_USER` está en `neo4j` en vez del id |
| el contenedor muere sin mensaje | poca memoria: revisar que `JAVA_OPTS` esté puesto |

---

## 3. Proteger la rama `main`

Como cada push a `main` redespliega, conviene que nadie empuje directo.

**Esto sólo lo puede hacer `jpsam4`, el dueño del repositorio.** Comprobado el
19/08/2026: la cuenta `joaquingit1` tiene `push` pero no `admin`, y la API
contesta `404 Not Found` al intentar configurar la protección (GitHub devuelve
404 en vez de 403 cuando falta el permiso de administrador).

**Settings → Branches → Add branch protection rule**

- Branch name pattern: `main`
- ✅ Require a pull request before merging → Require approvals: **1**
- ✅ Require status checks to pass before merging → elegir el check **`test`**
  (lo publica el workflow `ci.yml`)
- ✅ Require branches to be up to date before merging
- ❌ **No** tildar *Do not allow bypassing the above settings*, así el
  administrador sigue pudiendo empujar directo cuando haga falta

Con eso, el resto del equipo tiene que abrir pull request y esperar una
aprobación; el dueño del repo puede saltearlo.

### Limitación a tener en cuenta

En un repositorio **de cuenta personal** no se puede decir *"que sólo empuje
Fulano"*: la lista de quién puede empujar a una rama protegida existe únicamente
en repositorios **de organización**. Además, en repos personales los
colaboradores tienen todos el mismo nivel de acceso (escritura); no se puede
nombrar a otro administrador.

Si el equipo quiere el control fino de verdad, el camino es mover el repositorio
a una **organización de GitHub** (son gratis):

1. Crear la organización (plan Free).
2. `Settings → Transfer ownership` del repo hacia la organización.
   Se conservan el historial, los issues y las estrellas; las URLs viejas
   redirigen solas.
3. Asignar roles: Admin para quien coordina, Write para el resto.
4. Recién ahí, en la protección de rama, aparece
   **Restrict who can push to matching branches**.

Es media hora de trabajo y ordena el resto del cuatrimestre.
