# Bienvenue dans Markleaf

Markleaf est un carnet Markdown local-first pour Android. Il s'ouvre vite, reste discret et garde vos textes en plain text que vous possedez.

## Petit tour

- Ouvrez **Un beau canevas Markdown** pour voir la surface d'ecriture.
- Ouvrez **Rituel d'ecriture quotidien** pour un exemple de journal.
- Ouvrez **Brief de projet** pour les taches, liens et sections.
- Ouvrez **Miroir de dossier local** quand vous voulez des fichiers hors de l'app.

> [!TIP]
> Ce sont des notes normales. Modifiez-les, exportez-les, mettez-les a la corbeille ou supprimez-les quand vous n'en avez plus besoin.

#start #guide #debut

---markleaf-note---

# Un beau canevas Markdown

![Canevas d'exemple Markleaf](attachments/starter-note-2/markleaf-sample-cover.png)

Markdown reste lisible comme texte, puis devient calme et soigne en **Apercu**.

## Ce que montre cette note

- **Gras**, _italique_, ~~barre~~ et `code en ligne`
- Titres, listes, cases, citations, separateurs, blocs de code, tableaux, callouts, notes de bas de page, liens et images
- Style de syntaxe en direct pendant l'ecriture

> [!NOTE]
> Basculez entre Edition et Apercu dans la barre du haut. La note reste du Markdown.

| Element | Usage |
| --- | --- |
| `#tag` | organiser |
| `[[Brief de projet]]` | liens locaux |
| `![](...)` | images jointes |

```kotlin
fun markleaf() = "local-first markdown"
```

Une petite note de bas de page garde les details pres du texte.[^1]

[^1]: Notes de bas de page, callouts, tableaux et blocs de code sont rendus localement.

#markdown #showcase #ecriture

---markleaf-note---

# Rituel d'ecriture quotidien

## Page du matin

Le but n'est pas d'ecrire plus. Le but est de rendre la premiere phrase facile.

- [x] Capturer une idee
- [ ] Transformer une tache en note
- [ ] Lier le travail lie a [[Brief de projet]]

> Gardez la note assez petite pour avoir envie d'y revenir.

## Cloture du soir

Qu'est-ce qui a avance aujourd'hui ?

1. Une decision utile
2. Une question ouverte
3. Une chose pour demain

#journal #writing #journal

---markleaf-note---

# Brief de projet

Cette note montre comment Markleaf peut contenir un petit projet sans devenir lourd.

## Resultat

Livrer un carnet d'exemple clair qui enseigne par son utilite.

## Plan

- [x] Montrer Markdown joliment
- [x] Inclure une image jointe
- [ ] Chercher `local-first`
- [ ] Ouvrir les backlinks depuis **Rituel d'ecriture quotidien**

## Notes

Lie a : [[Rituel d'ecriture quotidien]] et [[Tags, recherche et backlinks]]

#project/markleaf #planning #projet

---markleaf-note---

# Tags, recherche et backlinks

Ecrivez les tags directement dans le corps : #project, #writing, #privacy, #local-first.

## Idees de recherche

Essayez de chercher :

- `local-first`
- `folder mirror`
- `Brief de projet`

## Backlinks

Les wikilinks utilisent `[[Titre de note]]`. Quand une autre note pointe ici, Markleaf peut montrer cette relation localement. Pas de compte, pas de serveur.

Voir aussi [[Brief de projet]].

#organize #search #organisation

---markleaf-note---

# Miroir de dossier local

Markleaf n'a pas besoin de son propre cloud. Vous choisissez un dossier, puis Android ou votre outil de synchronisation s'en occupe.

## Ce qui se passe

- Markleaf ecrit chaque note comme fichier Markdown.
- Le frontmatter garde le `markleaf_id` stable.
- Les pieces jointes restent a cote des notes miroir.
- L'app ne declare toujours aucune permission INTERNET.

## Pourquoi c'est important

Vos notes restent lisibles dans d'autres outils Markdown, et la synchro reste votre choix.

#privacy #folder-mirror #local-first #confidentialite
