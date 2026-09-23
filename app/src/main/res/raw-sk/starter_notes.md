# Vitajte v Markleaf

Markleaf je čistý, lokálny Markdown zápisník pre Android. Rýchlo sa otvára,
neprekáža a udržiava vaše písanie ako obyčajný text, ktorý patrí vám.

## Malá prehliadka

- Otvorte **Krásne prostredie Markdown-u**, aby ste videli pracovnú plochu.
- Otvorte si **Každodenný rituál písania**, kde nájdete príklady v štýle
  denníka.
- Otvorte **Stručný popis projektu**, aby ste si prezreli úlohy, odkazy a
  štruktúru.
- Otvorte **Zrkadlenie miestneho priečinka** ak potrebujete zdieľať súbory mimo
  aplikácie.

> [!TIP] Ide o bežné poznámky. Keď už prehliadku nebudete potrebovať, môžete ju
> upravovať, exportovať, presunúť do koša alebo vymazať.

#začiatok #návod

---markleaf-poznámka---

# Krásne prostredie Markdown-u

![Vzorový dokument
Markleaf](attachments/starter-note-2/markleaf-sample-cover.png)

Markdown zostáva čitateľný ako text a v **Náhľade** nadobúda prehľadný a
vycibrený vzhľad.

## Čo táto poznámka ukazuje

- **Tučné písmo**, _kurzíva_, ~~prečiarknuté~~ a `kód v riadku`
- Nadpisy, zoznamy, kontrolné zoznamy, citáty, oddeľovače, bloky kódu, tabuľky,
  vysvetlivky, poznámky pod čiarou, odkazy a obrázky
- Dynamické formátovanie syntaxe počas písania

> [!POZNÁMKA] V hornom paneli môžete prepínať medzi režimami „Upraviť“ a
> „Náhľad“. Poznámka je naďalej len v formáte Markdown.

| Prvok                        | Použite to na              |
| ---------------------------- | -------------------------- |
| `#značka`                    | organizácia                |
| `[[Stručný popis projektu]]` | odkazy na lokálne poznámky |
| `![](...)`                   | priložené obrázky          |

```kotlin
zábavný markleaf() = "prednostne lokálny markdown"
```

Malá poznámka pod čiarou umožňuje mať podrobnosti po ruke, bez toho, aby
narušovala plynulosť odseku.[^1]

[^1]: Poznámky pod čiarou, vysvetlivky, tabuľky a bloky kódu sa všetky zobrazujú
lokálne.

#markdown #prezentácia

---markleaf-poznámka---

# Každodenný rituál písania

## Ranná stránka

Cieľom nie je písať viac. Cieľom je, aby prvá veta bola jednoduchá.

- [x] Zachyť jednu myšlienku
- [ ] Premeniť jednu úlohu na poznámku
- [ ] Prepojiť súvisiace práce s [[Stručný popis projektu]]

> Ponechajte si poznámku dostatočne krátku, aby ste sa k nej skutočne vrátili.

## Záver dňa

Čo sa dnes zmenilo?

1. Jedno užitočné rozhodnutie
2. Jedna otvorená otázka
3. Jedna vec, ktorú si nechám na zajtra

#denník #písanie

---markleaf-poznámka---

# Stručný popis projektu

Táto poznámka ukazuje, ako Markleaf dokáže zvládnuť malý projekt bez toho, aby
sa stal ťažkopádnym.

## Výsledok

Vytvorte čistý ukážkový zošit, ktorý učí tým, že je užitočný.

## Plán

- [x] Zobraziť syntax Markdownu v atraktívnom formáte
- [x] Vložiť prílohu s obrázkom
- [ ] Skúste vyhľadávať s použitím `prednostne lokálne`
- [ ] Otvoriť spätné odkazy z **Každodenný rituál písania**

## Poznámky

Súvisiace: [[Každodenný rituál písania]] and [[Značky, vyhľadávanie a spätné
odkazy]]

#projekt/markleaf #plánovanie

---markleaf-poznámka---

# Značky, vyhľadávanie a spätné odkazy

Zadávajte značky priamo do textu: #projekt, #písanie, #súkromie,
#prednostne_lokálne.

## Nápady na vyhľadávanie

Skúste vyhľadať:

- `prednostne lokálne`
- `zrkadlenie priečinka`
- `Stručný popis projektu`

## Spätné odkazy

Vikilinks používajú formát `[[Názov poznámky]]`. Ak sem odkazuje iná poznámka,
Markleaf môže tento vzťah zobraziť priamo v danej poznámke. Nie je do toho
zapojený žiadny účet ani server.

Pozri tiež [[Stručný popis projektu]].

#organizovať #vyhľadávať

---markleaf-poznámka---

# Zkradlenie lokálneho priečinka

Markleaf nepotrebuje vlastný cloud. Namiesto toho si môžete vybrať priečinok a
nechať, aby sa oň postaral systém Android alebo váš synchronizačný nástroj.

## Čo sa stane

- Markleaf ukladá každú poznámku ako súbor vo formáte Markdown.
- Úvodná sekcia zachováva nemenné `markleaf_id`.
- Prílohy sa ukladajú vedľa zrkadlených poznámok.
- Markleaf nikdy neukladá vaše poznámky – synchronizácia je úlohou vášho
  nástroja, nie aplikácie.

## Prečo je to dôležité

Vaše poznámky zostávajú čitateľné aj v iných nástrojoch na prácu s Markdownom a
synchronizácia závisí od vášho rozhodnutia.

#ochrana súkromia #zrkadlenie priečinka #prednostne-lokálne
