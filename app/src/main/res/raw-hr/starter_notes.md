# Dobro došli u Markleaf

Markleaf je tiha, primarno lokalna bilježnica za Markdown za Android. Brzo se otvara, ne smeta i sprema Vaše pisanje kao obični tekst kojem ste Vi vlasnik.

## Mali obilazak

- Otvorite **Lijepo platno za Markdown** da se upoznate s površinom za pisanje.
- Otvorite **Dnevni ritual pisanja** za primjer u obliku dnevnika.
- Otvorite **Kratki opis projekta** da vidite zadatke, poveznice i strukturu.
- Otvorite **Zrcaljenje lokalne mape** kada želite datoteke izvan aplikacije.

> [!TIP]
> Ovo su obične bilješke. Uredite ih, izvezite, premjestite u smeće ili izbrišite kada više ne trebate obilazak.

#početak #vodič

---markleaf-note---

# Lijepo platno za Markdown

![Markleaf sample canvas](attachments/starter-note-2/markleaf-sample-cover.png)

Markdown ostaje čitljiv kao tekst, zatim postaje miran i uglađen u **Pretpregledu**.

## Što ova bilješka predstavlja

- **podebljanje**, _kurziv_, ~~precrtavanje~~ i `kod unutar teksta`
- naslove u tekstu, popise, popise zadataka, citate, razdjelnike, blokove koda, tablice, istaknute okvire, fusnote, poveznice i slike
- stiliziranje sintakse u stvarnom vremenu kako pišete

> [!NOTE]
> Prebacite se između načina za uređivanje i pretpregleda pomoću gornje trake. Bilješka je i dalje samo Markdown.

| Element                    | Koristite ga za               |
|----------------------------|-------------------------------|
| `#oznaka`                  | organizaciju                  |
| `[[Kratki opis projekta]]` | poveznice na lokalne bilješke |
| `![](...)`                 | priložene slike               |

```kotlin
fun markleaf() = "primarno lokalni markdown"
```

Mala fusnota zadržava detalje u blizini, bez da prekida odlomak.[^1]

[^1]: Fusnote, istaknuti okviri, tablice i blokovi koda se renderiraju lokalno.

#markdown #izlog

---markleaf-note---

# Dnevni ritual pisanja

## Jutarnja stranica

Cilj nije pisati više. Cilj je učiniti prvu rečenicu jednostavnom.

- [x] Zabilježite jednu misao
- [ ] Pretvorite jedan zadatak u bilješku
- [ ] Povežite povezani rad s [[Kratki opis projekta]]

> Održavajte bilješku dovoljno malom da joj se zapravo želite vratiti.

## Večernji predah

Što je pokrenulo današnji dan?

1. Jedna korisna odluka
2. Jedno otvoreno pitanje
3. Jedna stvar ostavljena za sutra

#dnevnik #pisanje

---markleaf-note---

# Kratki opis projekta

Ova bilješka prikazuje kako Markleaf može sadržavati mali projekt bez da postaje nespretan za korištenje.

## Ishod

Izbaciti na tržište čistu i jednostavnu bilježnicu koja te uči tako što je korisna.

## Plan

- [x] Lijepo prikazati Markdown sintaksu
- [x] Uključiti priložene slike
- [ ] Isprobati pretragu s `primarno-lokalno`
- [ ] Otvoriti povratne poveznice iz **Dnevni ritual pisanja**

## Bilješke

Povezano: [[Dnevni ritual pisanja]] i [[Oznake, pretraživanje i povratne poveznice]]

#projekt/markleaf #planiranje

---markleaf-note---

# Oznake, pretraživanje i povratne poveznice

Upišite oznake izravno u tijelu bilješke: #projekt, #pisanje, #privatnost, #primarno-lokalno.

## Traženje ideja

Pokušajte pretražiti:

- `primarno-lokalno`
- `zrcaljenje mape`
- `Kratki opis projekta`

## Povratne poveznice

Wikipoveznice koriste `[[Naslov bilješke]]`. Kada druga bilješka vodi na ovu, Markleaf može prikazati taj odnos lokalno. Bez računa ili poslužitelja.

Pogledajte i [[Kratki opis projekta]].

#organizacija #pretraživanje

---markleaf-note---

# Zrcaljenje lokalne mape

Markleaf ne zahtijeva vlastiti oblak. Umjesto toga možete odabrati mapu i prepustiti Androidu ili Vašem alatu za sinkronizaciju da zrcali tu mapu.

## Što se događa

- Markleaf zapisuje svaku bilješku kao Markdown datoteku.
- Frontmatter održava stabilni `markleaf_id`.
- Priložene datoteke ostaju uz zrcaljene bilješke.
- Aplikacija i dalje ne zahtijeva dozvolu za INTERNET.

## Zašto je to važno

Vaše bilješke ostaju čitljive i u drugim alatima za Markdown, a sinkronizacija ostaje kao Vaša odluka.

#privatnost #zrcaljenje-mape #primarno-lokalno
