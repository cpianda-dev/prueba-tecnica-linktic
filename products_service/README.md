# Products Service

Este microservicio maneja los productos de la aplicación.  
Actualmente está preparado para correr con Docker y Docker Compose junto a su base de datos.

---

## Requisitos

- [Docker](https://www.docker.com/get-started)
- [Docker Compose](https://docs.docker.com/compose/install/)
- Java 17 (solo si se ejecuta local sin Docker)

---

## Levantar el microservicio con Docker

1. Construir y levantar todos los contenedores (microservicio + DB):

```bash
docker compose up -d --build
```

> ⚠️ Nota: Si ya existe un contenedor con el mismo nombre, puede fallar. En ese caso:

```bash
docker rm -f products_db
docker compose up -d --build
```

2. Para levantar solo el microservicio (sin tocar la DB):

```bash
docker compose up -d --build products_service
```

---

## Ver logs

- Todos los servicios:

```bash
docker compose logs -f
```

- Solo el microservicio:

```bash
docker compose logs -f products_service
```

---

## Detener los contenedores

```bash
docker compose down
```

---

## Limpieza de contenedores y redes no usados

```bash
docker system prune -f
```

---
