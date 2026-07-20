---
name: gwt-pseudo-code-test-to-minimal-implementation
description: Recibe un test en pseudocódigo Given/When/Then y lo vuelve realidad en Java — un test excelente y la implementación mínima suficiente para que pase. Úsalo cuando el usuario pegue un bloque given_/when_/then_ y quiera verlo en verde.
argument-hint: el bloque given_/when_/then_ (opcionalmente + ruta de un archivo como contexto)
---

# GWT en pseudocódigo → test real + implementación mínima

Tu trabajo es tomar un Given/When/Then escrito en pseudocódigo y **dejarlo corriendo en
verde**. Dos entregables con estándares distintos, y esa asimetría es intencional:

| | Estándar |
|---|---|
| **El test** | Es el entregable cuidado. Expresivo, con las mejores prácticas, del largo que haga falta. |
| **La implementación** | Lo **mínimo suficiente** para que ese test pase. No basura: suficiente. |

La implementación no busca ser bonita porque **habrá otro skill que haga el refactor**.
Estar tranquilo con eso es parte del trabajo. Menos código de producción es también menos
entropía: no inventes capas, interfaces, ni abstracciones que el test no pidió.

---

## Regla número uno: ante la duda, pregunta

**Las ambigüedades son peligrosas y preguntarlas no es opcional.** El usuario puede saber
mucho de Java o nada, y puede saber mucho del negocio o nada. No asumas ninguna de las dos
cosas.

Pregunta siempre que:

- El pseudocódigo admita **más de una lectura de negocio**.
- No quede claro **qué debería pasar** en un caso.
- Vayas a usar una característica de Java con **efectos colaterales** que el usuario podría
  no esperar (mutabilidad, autoboxing, `equals`/`hashCode`, zonas horarias, división entera,
  comparación de flotantes, orden no garantizado, etc.). Menciónalo y **confírmalo**.
- No sepas dónde poner algo sin ensuciar el repo.

### El caso canónico

Este ejemplo viene del usuario y fija el criterio:

```
amount = 50
given_that_amount_is_positive(amount)
result = when_i_divide_amount_by_2(amount)
then_result_is(50//25)
```

Con `amount = 50`, dividir entre 2 da **25**. Pero la aserción dice `50//25`, que da **2**.
No se sabe si quiso decir `50//2`, o `25`, o algo más.

**Aquí se para y se pregunta.** No se elige la lectura más probable y se sigue. Si este
skill recibiera ese bloque y no preguntara, estaría mal hecho.

---

## Paso 1: entender el entorno

1. **Detecta la versión de Java** (`pom.xml`, `build.gradle`, `.sdkmanrc`, `.tool-versions`).
   - Si la encuentras: **confírmala** con el usuario ("veo Java 21, ¿lo confirmas?").
   - Si no: **pregúntala**. No adivines: cambia qué puedes usar (records, `switch` con
     patrones, `var`, text blocks).
2. **Si el repo ya tiene código, explora sus convenciones antes de escribir nada.**
   Estructura de paquetes, cómo se nombran las clases, qué framework de test usa (JUnit 4
   vs 5), si hay AssertJ o Hamcrest, si usan Lombok. **Sigue lo que ya existe.** El usuario
   te va a pedir que aun así avientes el código más simple y rápido posible, y eso está
   bien, pero simple no significa incoherente con el repo.
3. **Si te pasaron un archivo como contexto, léelo completo** antes de proponer nada.

---

## Paso 2: leer el pseudocódigo

El usuario escribe como le sale. Puede llegarte snake_case, Java, o una mezcla. Puede
inventar artefactos que no habíamos previsto. **Infiere lo que quiso decir, y lo que no
puedas inferir, pregúntalo.**

Lo que sí es fijo:

- `given_that_amount_is_positive` → `givenThatAmountIsPositive`. **camelCase, porque es Java**,
  conservando el prefijo `given`/`when`/`then`.
- Las asignaciones (`amount = 50`, `result = when_i_...`) son **variables locales del test**.
- Los `given_`, `when_`, `then_` son **métodos privados del archivo de test**.
- El cuerpo del `@Test` contiene **sólo** esas llamadas. Sin setup inline, sin `assertThat`
  suelto, sin factories a pelo.

**Respeta la estructura que te dieron.** Si no se puede tal cual, hazlo como se pueda y
**di qué cambiaste y por qué**.

---

## Paso 3: escribir el test

Este es el entregable que sí se cuida.

- Nombre del método: `shouldX_whenY`.
- Un `when_` por test. Varios `then_` está bien.
- `@Nested` + `@DisplayName` si el pseudocódigo trae contextos anidados.
- Que se lea como una historia. Alguien que no sepa Java debería entender el `@Test`.

**El test puede ser largo.** No lo recortes por recortarlo. Si ves que podría quedar más
corto o más claro, **coméntalo y pregunta si lo quiere así**, explicando la ventaja. La
decisión es del usuario.

También puedes **criticar el pseudocódigo** que te dieron: si un `then_` prueba dos cosas,
si falta un caso obvio, si el nombre miente sobre lo que hace. Dilo. No lo arregles en
silencio.

---

## Paso 4: escribir la implementación

Lo mínimo que haga pasar ese test.

- **En repo vacío:** el archivo que se te haga más natural. Di dónde lo pusiste.
- **En repo existente:** la **primera clase disponible donde no sea caótico empezar a
  definir esa lógica**. No una clase nueva por cada cosa, pero tampoco metas lógica de
  negocio en un controlador si al lado hay un lugar evidente. Si dudas entre dos lugares,
  **pregunta**.
- **Test e implementación van en archivos distintos.** En Java es así, y no es una
  discusión de estilo.
- Sin interfaces, sin fábricas, sin capas que el test no haya pedido.

---

## Paso 5: pelear por el verde

**Con garras y dientes.** No entregues sin haber corrido el test.

```bash
./mvnw test -Dtest=NombreDeLaClaseDeTest
```

Si sale rojo, itera. Si el rojo revela que el pseudocódigo era ambiguo o contradictorio,
**para y pregunta** en vez de torcer la implementación para que pase.

Al terminar, reporta en dos líneas: qué archivos tocaste, y qué comando corriste para
comprobarlo.

---

## Lo que este skill NO hace

- No refactoriza. Otro skill se encarga.
- No agrega tests que nadie pidió.
- No mejora la implementación "de paso".
- No decide por el usuario cuando algo es ambiguo.
