# Better Mob Particles — для выкладки на Modrinth

---

# ЧАСТЬ 1. Поля формы (не часть описания)

| Поле | Значение |
|---|---|
| **Name** | Better Mob Particles |
| **Slug** | `better-mob-particles` |
| **Summary** | Mobs throw off what they're made of when you hit them — blood, bone, sparks, ash. Any weapon, any mod. |
| **Type / Loader** | Mod / Fabric |
| **Game versions** | 26.2 |
| **Client side** | **Required** |
| **Server side** | **Required** |
| **License** | MIT |
| **Categories** | Decoration, Mobs |

**Зависимости:** Fabric API — Required · Cloth Config — Required · Mod Menu — Optional

**Ссылки в форме:** Source code — `https://github.com/Mixaold/BetterMobParticles`, Issue tracker — тот же адрес с `/issues`. Wiki и Discord — пусто. Donation — DonationAlerts через «Other».

**Client и Server оба Required** потому, что мод регистрирует свой тип частицы. Если версии не совпадут — клиента выкинет на входе с ошибкой синхронизации реестров.

**Скриншоты в галерею:** скелет (костная крошка), блейз (пламя + угольки), железный голем (крошка + искры), капли крови на земле, экран настроек.

**Иконка проекта:** `icons/icon-512.png`.

---
---

# ЧАСТЬ 2. Описание (English)

## Better Mob Particles

Hit a mob in vanilla and it flashes red for a moment. That's the whole feedback you get.

This mod makes hits look like hits. Whatever you hit throws off the stuff it's made of — blood from
flesh, bone chips from skeletons, sparks from a blaze, ash from a ghast, a puff of air from a
breeze. Blood flies out of the wound in the direction of the hit, falls, lands on the ground and
slowly fades away.

Works with any weapon from any mod, plus plain vanilla arrows and your bare fists.

### What comes out of what

| Mob | What flies out |
|---|---|
| Everything else, players included | Red blood |
| Skeletons | Bone chips |
| Blaze, magma cube, wither | Flame and lava embers |
| Iron golem | Iron chips and sparks |
| Copper golem | Copper chips |
| Snow golem | Snowflakes |
| Slime | Slime |
| Ghast, happy ghast | White ash |
| Breeze | A puff of air, straight up |
| Sulfur cube | Sulfur |
| Creaking | Wood chips |
| Armor stand, ender dragon, vex, allay, shulker, enderman | Nothing |

### When it happens

* **Arrow, trident or any projectile** — always.
* **Sword, axe, pickaxe, shovel, hoe, trident, mace** — always.
* **Bare fist, a block, a torch** — sometimes, and less of it.

Nothing comes out if the hit was blocked by a shield, or from fall damage, fire, drowning and
poison — there is no wound to bleed from.

### Settings

**Mod Menu → Better Mob Particles → Settings.** How many particles, how long blood lies on the
ground, how far it sprays, and whether melee hits count at all. Everything can be turned off.

> ### ⚠️ Install it on the client AND the server
> Same version on both sides, or the client gets kicked at login. If a friend can't join — they've
> got an old version. Give them the same jar you're running.

### Requires

Fabric · Minecraft 26.2 · Fabric API · Cloth Config · Mod Menu (optional, for the settings button)

MIT license — use it, fork it, put it in your modpack.

---
---

# ЧАСТЬ 3. Описание (Русский)

## Better Mob Particles

Бьёшь моба в ванили — он на мгновение краснеет. Это вся обратная связь.

Мод делает так, что удар выглядит ударом. Из того, по кому ты попал, вылетает то, из чего он
сделан: кровь из плоти, костная крошка из скелета, искры из блейза, пепел из гаста, порыв воздуха
из бриза. Кровь вылетает из раны по направлению удара, падает, ложится на землю и медленно тает.

Работает с любым оружием из любого мода, а ещё с обычными стрелами и голыми кулаками.

### Из кого что летит

| Моб | Что вылетает |
|---|---|
| Все остальные, включая игроков | Красная кровь |
| Скелеты | Костная крошка |
| Блейз, лавовый куб, иссушитель | Пламя и угольки лавы |
| Железный голем | Крошка железа и искры |
| Медный голем | Крошка меди |
| Снежный голем | Снежинки |
| Слизень | Слизь |
| Гаст, дружелюбный гаст | Белый пепел |
| Бриз | Порыв воздуха, вверх |
| Серный куб | Сера |
| Крипящий | Древесная крошка |
| Стойка для брони, дракон Края, вредина, аллей, шалкер, эндермен | Ничего |

### Когда это происходит

* **Стрела, трезубец, любой снаряд** — всегда.
* **Меч, топор, кирка, лопата, мотыга, трезубец, булава** — всегда.
* **Кулак, блок, факел** — иногда и поменьше.

Ничего не вылетает, если удар заблокировали щитом, а также от падения, огня, утопления и яда —
там нет раны.

### Настройки

**Mod Menu → Better Mob Particles → Настройки.** Сколько частиц, сколько кровь лежит на земле,
как далеко разлетается и считаются ли удары в ближнем бою. Всё можно выключить.

> ### ⚠️ Ставить и на клиент, и на сервер
> Версия должна быть одна и та же с обеих сторон, иначе клиента выкинет на входе. Если друг не
> может зайти — у него старая версия. Дай ему тот же jar, что стоит у тебя.

### Требуется

Fabric · Minecraft 26.2 · Fabric API · Cloth Config · Mod Menu (по желанию, ради кнопки настроек)

Лицензия MIT — пользуйтесь, форкайте, кладите в сборки.
