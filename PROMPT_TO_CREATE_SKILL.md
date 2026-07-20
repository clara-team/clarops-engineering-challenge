1. buena pregunta profe, en el mejor archivo que se le ocurra; evidentemente el test y la implementación están en distintos lugares; esto no cuenta como buena o mala práctica,
  más bien conocimiento de java y en eso el skill es magnifico. La versión de java sí la debe de preguntar eh.
2. No estará asociado directamente a ningún archivo de tests, pero se le podría pasar como contexto. Más bien recibe el texto, por ejemplo:
      given_that_today_is_today
      when_i_request_a_hello
      then_a_hello_was_printed_into_the_console
    podrían enviarse más detalles de pseudocódigo que el skill tendrá que modelar, por ejemplo
      amount = 50
      given_that_amount_is_positive(amount)
     result = when_i_divide_amount_by_2(amount)
      then_result_is(50//25)
    también debe de pasarse en este caso a camel case, porque Java; pero debe de estarse consciente de que esto es pseudocódigo; incluso si mando Java
    ** el skill intentará respetar la estructura del test de pseudocódigo, pero si no se puede, no se puede y que lo haga como pueda
    ** a diferencia de la implementación, el código de test debe de ser corto, expresivo y siguiendo las mejores pŕacticas
    ** en cualquier momento el skill puede preguntar al usuario cualquier cosa, porque el usuario podría saber de JAVA(PODRÍA NO SABER, TAMBIÉN)
    ** de lo que el skill debe de estar consciente es de que tal vez el usuario no sepa del negocio, entonces necesita mantenerse en comunicación con él para evitar
  ambvifuedades
    ** el hecho de que se pida que el skill aviente poco código, es para minimizar un poco la entropía del llm
    ** si el skill necesita trabajar en una base de código que ya existe, debe de primero explorar las convenciones del código y estar en comunicación con el usuario porque
  probablemente, le pedirá que aún así aviente código lo más simple y rápido posible, en la primer clase disponible; o mejor aún: en la primer clase disponible que no sea
  caótico comenzar a definir la lógica
    ** el skill y el usuario deben de estar tranquilos porque la tarea del skill tampoco es generar código basura, sino generar código SUFICIENTE; tendremos otro skill que hará
  refactors.



¿Me confirmaría lo que entiende de mi petición ANTES de comenzar? Confírmemelo en forma de pregunta. Valide conmigo antes de comenzar.

==============================

Confirmo en forma de pregunta. Y una observación al final que creo que le va a gustar.

1. ¿La inversión es a propósito? Entiendo que el test debe ser corto, expresivo y con las mejores prácticas, mientras que la implementación debe ser lo mínimo suficiente para que ese test pase, viva donde viva. O sea: el test es el entregable cuidado, la implementación es el andamio que lo sostiene. ¿Correcto?

2. ¿La versión de Java siempre se pregunta, aunque el skill pueda detectarla del pom.xml (aquí dice 21)? ¿O se detecta y se confirma con el usuario, tipo "veo Java 21, ¿lo confirmas?"?

3. Sobre el pseudocódigo: entiendo que convierte given_that_amount_is_positive a givenThatAmountIsPositive, conservando los prefijos given/when/then como métodos privados, y modelando las asignaciones (amount = 50, result = when_...) como variables locales del test. ¿Así?

4. ¿Cuándo pregunta y cuándo asume? Mi lectura: pregunta cuando la ambigüedad es de negocio (qué significa el caso, qué debería pasar) o cuando una decisión de Java cambia el comportamiento; no pregunta por trivialidades de sintaxis o de estilo. ¿Ese es el umbral?

5. Dos que me quedaron pendientes de la ronda anterior:
- ¿El skill corre el test hasta verde, o sólo escribe los archivos?
- ¿Va en clear_challenge/skills/... con symlink desde clear_challenge/.claude/skills/?

---
Y la observación: tu propio ejemplo trae la ambigüedad que el skill tiene que cazar.

result = when_i_divide_amount_by_2(amount)
then_result_is(50//25)

Con amount = 50, dividir entre 2 da 25. Pero escribiste 50//25, que da 2. No sé si querías 50//2 (división entera estilo Python) o directamente 25.

Eso es exactamente el momento en que el skill debe parar y preguntar, en vez de elegir uno y seguir. Y me sirve como caso de prueba del propio skill: si le paso ese GWT tal cual y no pregunta, el skill está mal hecho.

¿Lo dejo como ejemplo dentro del SKILL.md, para fijar el criterio de cuándo preguntar?

============================

 1. El skill puede hacer criticas al código y tal vez confirmar si realmente el usuario quiere eso, mencionando las ventajas de test cortos (recuerde que el skill sí escribirá
  código de test excelente) pero el test NO tiene porque ser corto, puede ser largo.
  2. Que pregunte si no sabe, que confirme si sí sabe
  3. Sí profe y aún más tipos de artifactos que al usuario se le pueden ocurrir, el skill deberá inferir a qué se refiere el usuario o preguntarle
  4. Cualquier duda que tenga el skill, que pregunte. Las ambiguedades son preligrosas, DEBE peguntarlas. Y si conoce side effects de alguna característica de java siendo usada,
  que lo confirme con el usuario también.
  5.1 El skill luchará con garras y dientes por ese color verde.
  5.2 Sí.

  Sobre la observación: Me cachó profe, me equivoqué, estoy cansado. De acuerdo con su  observación profe.

¿Me confirmaría lo que entiende de mi petición ANTES de comenzar? Confírmemelo en forma de pregunta. Valide conmigo antes de comenzar.

=============================



Entendido. Te lo devuelvo completo y compacto, y con una sola pregunta al final.

El contrato del skill, como yo lo entiendo:

1. Se llama /gwt-pseudo-code-test-to-minimal-implementation, vive en clear_challenge/skills/ y le hago symlink desde clear_challenge/.claude/skills/.
2. Recibe texto GWT en pseudocódigo. Puede traer asignaciones, llamadas, y artefactos que el usuario invente sobre la marcha. Puede venir en snake_case, en Java, o en algo intermedio. El skill infiere o pregunta, y traduce a camelCase porque es Java.
3. El test es el entregable cuidado: expresivo, con las mejores prácticas, y de la longitud que haga falta. Si el skill ve que podría ser más corto, lo comenta y confirma, explicando la ventaja, pero no lo impone.
4. La implementación es lo mínimo suficiente para que ese test pase. No basura: suficiente. Va donde estorbe menos, y en código existente eso significa la primera clase disponible donde no sea caótico empezar a poner lógica, después de explorar las convenciones del repo.
5. Test e implementación van en archivos distintos, porque en Java así es. Eso no es una preferencia de estilo, es cómo funciona.
6. La versión de Java se detecta y se confirma; si no se puede detectar, se pregunta.
7. Ante cualquier duda, pregunta. Las ambigüedades de negocio son las peligrosas y son obligatorias de preguntar. Y si usa alguna característica de Java con efectos colaterales que el usuario podría no esperar, también lo confirma.
8. Pelea con garras y dientes por el verde: escribe, corre el test, y si sale rojo itera hasta que pase.
9. Sabe que no es su trabajo dejarlo bonito. Habrá otro skill para el refactor.
10. En el SKILL.md queda tu ejemplo del 50//25 como criterio de cuándo parar y preguntar.

¿Algo de esos diez está mal o incompleto? Si no, lo escribo de una vez.


=====================
Escríbalo profe. 

====================
Escrito y enlazado.

- El skill: clear_challenge/skills/gwt-pseudo-code-test-to-minimal-implementation/SKILL.md
- El symlink: clear_challenge/.claude/skills/ → apunta ahí

Tres decisiones que tomé al redactarlo y que conviene que veas:

1. La asimetría va en una tabla al principio. Es lo más raro del skill (test excelente, implementación mínima) y si no se dice de entrada, el modelo se va a inclinar a tratar los dos igual.
2. "Ante la duda, pregunta" quedó como regla número uno, antes de los pasos, con tu ejemplo del 50//25 como caso canónico. Lo dejé con la frase que dijiste: si este skill recibiera ese bloque y no preguntara, estaría mal hecho. Sirve de prueba del propio skill.
3. Agregué una lista concreta de efectos colaterales de Java que debe confirmar (mutabilidad, autoboxing, equals/hashCode, zonas horarias, división entera, comparación de flotantes, orden no garantizado). Sin esa lista, "confirma los efectos colaterales" es una instrucción que nadie sigue.

Para probarlo, el mejor primer caso es justo el tuyo del 50//25: si pregunta en vez de elegir, el skill sirve. Y si quieres romper el hielo de verdad, el segundo caso es T1, que ya tiene su GWT escrito en el spec.

Descansa, profe. Mañana a las 9.

=======================
