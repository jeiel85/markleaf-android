# Vitajte v Markleaf

Markleaf je pokojný, prednostne lokálny zápisník Markdown pre Android. Rýchlo sa otvára, neprekáža a uchováva vaše písanie ako obyčajný text, ktorý patrí vám.

## Malá prehliadka

- Otvorte **Krásne plátno Markdownu** a pozrite si pracovnú plochu.
- Otvorte **Každodenný rituál písania**, kde nájdete príklad v štýle denníka.
- Otvorte **Stručný popis projektu** a pozrite si úlohy, odkazy a štruktúru.
- Otvorte **Zrkadlenie lokálneho priečinka**, keď chcete mať súbory aj mimo aplikácie.

> [!TIP]
> Toto sú bežné poznámky. Upravte ich, exportujte, presuňte do koša alebo ich vymažte, keď prehliadku už nebudete potrebovať.

#začiatok #návod

---markleaf-note---

# Krásne plátno Markdownu

![Ukážkové plátno Markleaf](attachments/starter-note-2/markleaf-sample-cover.png)

Markdown zostáva čitateľný ako text a v **Náhľade** nadobúda pokojný, vycibrený vzhľad.

## Čo táto poznámka ukazuje

- **Tučné písmo**, _kurzíva_, ~~prečiarknutie~~ a `kód v riadku`
- Nadpisy, zoznamy, zoznamy úloh, citáty, oddeľovače, bloky kódu, tabuľky, zvýraznené bloky, poznámky pod čiarou, odkazy a obrázky
- Zvýrazňovanie syntaxe už počas písania

> [!NOTE]
> Medzi úpravou a náhľadom prepínate v hornom paneli. Poznámka je stále len Markdown.

| Prvok | Na čo slúži |
| --- | --- |
| `#značka` | organizácia |
| `[[Stručný popis projektu]]` | odkazy na lokálne poznámky |
| `![](...)` | priložené obrázky |

```kotlin
fun markleaf() = "prednostne lokálny markdown"
```

Malá poznámka pod čiarou drží podrobnosti nablízku bez toho, aby prerušila odsek.[^1]

[^1]: Poznámky pod čiarou, zvýraznené bloky, tabuľky aj bloky kódu sa vykresľujú lokálne.

#markdown #ukážka

---markleaf-note---

# Každodenný rituál písania

## Ranná stránka

Cieľom nie je písať viac. Cieľom je, aby prvá veta bola jednoduchá.

- [x] Zachytiť jednu myšlienku
- [ ] Premeniť jednu úlohu na poznámku
- [ ] Prepojiť súvisiacu prácu so [[Stručný popis projektu]]

> Nechajte poznámku dosť krátku na to, aby ste sa k nej naozaj vrátili.

## Záver dňa

Čo sa dnes pohlo dopredu?

1. Jedno užitočné rozhodnutie
2. Jedna otvorená otázka
3. Jedna vec, ktorú nechám na zajtra

#denník #písanie

---markleaf-note---

# Stručný popis projektu

Táto poznámka ukazuje, ako Markleaf zvládne malý projekt bez toho, aby sa stal ťažkopádnym.

## Výsledok

Vydať čistý ukážkový zápisník, ktorý učí tým, že je užitočný.

## Plán

- [x] Pekne zobraziť syntax Markdownu
- [x] Vložiť prílohu s obrázkom
- [ ] Skúsiť vyhľadať `prednostne lokálne`
- [ ] Otvoriť spätné odkazy z **Každodenný rituál písania**

## Poznámky

Súvisiace: [[Každodenný rituál písania]] a [[Značky, vyhľadávanie a spätné odkazy]]

#projekt/markleaf #plánovanie

---markleaf-note---

# Značky, vyhľadávanie a spätné odkazy

Značky píšte priamo do textu: #projekt, #písanie, #súkromie, #prednostne-lokálne.

## Nápady na vyhľadávanie

Skúste vyhľadať:

- `prednostne lokálne`
- `zrkadlenie priečinka`
- `Stručný popis projektu`

## Spätné odkazy

Wikiodkazy majú tvar `[[Názov poznámky]]`. Keď na túto poznámku odkazuje iná poznámka, Markleaf môže tento vzťah zobraziť lokálne. Nie je do toho zapojený žiadny účet ani server.

Pozrite tiež [[Stručný popis projektu]].

#organizácia #vyhľadávanie

---markleaf-note---

# Zrkadlenie lokálneho priečinka

Markleaf nepotrebuje vlastný cloud. Namiesto toho si môžete vybrať priečinok a nechať, aby sa oň postaral Android alebo váš synchronizačný nástroj.

## Čo sa deje

- Markleaf zapisuje každú poznámku ako súbor Markdown.
- Úvodná hlavička (frontmatter) uchováva stály `markleaf_id`.
- Prílohy zostávajú vedľa zrkadlených poznámok.
- Markleaf nikdy neodosiela vaše poznámky — synchronizácia je úlohou vášho nástroja, nie aplikácie.

## Prečo na tom záleží

Vaše poznámky zostávajú čitateľné aj v iných nástrojoch pre Markdown a o synchronizácii rozhodujete vy.

#súkromie #zrkadlenie-priečinka #prednostne-lokálne
