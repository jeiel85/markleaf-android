// The language menu is a plain <details> element, so it opens and closes
// without this file. This only adds what a menu is expected to do on top:
// close on Escape (returning focus to its button) and on a click elsewhere.
document.addEventListener('click', (event) => {
    for (const menu of document.querySelectorAll('details.lang-menu[open]')) {
        if (!menu.contains(event.target)) menu.open = false;
    }
});

document.addEventListener('keydown', (event) => {
    if (event.key !== 'Escape') return;
    for (const menu of document.querySelectorAll('details.lang-menu[open]')) {
        menu.open = false;
        menu.querySelector('summary').focus();
    }
});
