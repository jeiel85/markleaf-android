# Using your notes on a computer — folder sync guide

Markleaf is an Android app, and it stays one: there is no desktop or web
version (see `docs/AGENT_SPEC.md` §15.5). You don't need one to work on your
notes from a computer. Folder sync writes every note as a plain `.md` (or
`.txt`) file into a folder you choose, so any editor on Linux, macOS or
Windows can open the same files, and any sync tool can carry them between
devices.

Markleaf never sends anything over the network. Moving the folder between
devices is the job of the sync app you pick. This guide uses
[Syncthing](https://syncthing.net/) as the example, because it is open source,
runs on Linux, and syncs directly between your own devices with no account
and no server.

## 1. Set up the folder on the phone

1. Install a Syncthing app on the phone. The official Syncthing Android app
   was discontinued in 2024; at the time of writing (October 2026) the
   maintained build is **Syncthing-Fork**, on
   [F-Droid](https://f-droid.org/packages/com.github.catfriend1.syncthingfork/).
2. Create a folder for your notes, for example `Notes/` in the phone's main
   storage, and share it in Syncthing with your computer.
3. In Markleaf, open **Settings → Sync & export** and tap **Choose sync folder**.
   Pick the same folder. Android asks you to allow Markleaf to use it; that
   permission covers this one folder only.

Markleaf then reads in any Markdown or text files already in the folder, and
writes out every note that isn't locked. Locked notes never leave the app.

## 2. Set up the computer

1. Install Syncthing (on most Linux distributions it is in the package
   manager, for example `apt install syncthing`) and accept the folder shared
   from the phone.
2. Open that folder in any editor: Obsidian, VS Code, Zettlr, ghostwriter,
   `vim`, or a file manager. Markleaf's files are ordinary Markdown.

## 3. How the two sides stay in step

- **Phone → folder.** Markleaf saves a note to its file about a second after
  each edit.
- **Folder → phone.** Markleaf reads changed files when you bring the app to
  the foreground (at most once a minute) and when you tap **Sync now** in the
  Sync Center. Syncthing has to have delivered the file first.
- **Both sides edited the same note.** When the file changed on the computer
  *and* the note changed on the phone since the last sync, Markleaf keeps
  both: your phone's version stays the note, and the computer's version
  arrives as a separate note marked as a copy from another device. The Sync
  Center lists these copies so you can compare and delete the one you don't
  want.
- **Deleting a file never deletes a note.** Deleting a file on the computer
  leaves the note in Markleaf, and the file comes back the next time you edit
  the note. This is deliberate: a sync tool that is mid-transfer must never
  be able to delete a note. The other direction does reach the folder:
  deleting a note for good from Markleaf's Trash also deletes its file, and
  your sync app passes that on to the computer.
- **Only the top level is read.** Files in subfolders are not imported yet.
  Attachments live in an `attachments/` subfolder that Markleaf manages.

## 4. File names

Markleaf names a file after the note's title: its first heading, or its first
line, depending on **Settings → Notes → Note title**. Characters that some
file systems refuse (`\ / : * ? " < > |`) are replaced with spaces, and a
second note with the same title gets ` (2)` after its name.

By default the name **follows the title**: retitle the note and the file is
renamed to match. That also means a name you give the file on the computer is
undone the next time you edit that note in Markleaf.

If you name your files yourself, for example short names you manage from a
shell, turn on **Keep file names** in the Sync Center (Settings → Sync &
export → Sync Center). Then:

- a new note's file is still named after its title. While you write the note
  for the first time, the name follows the title as you type it;
- from the next time you open the note, Markleaf never renames it. Retitling
  the note changes the title in the app, not the file name;
- a name you give the file outside Markleaf (`mv "A long first line.md"
  groceries.md`) stays, and Markleaf keeps writing into `groceries.md`;
- **Rename files to note titles** is hidden, since it would undo every name
  at once.

If your Markleaf doesn't show **Keep file names** yet, update the app.

## 5. Header or no header

Markleaf has to know which file belongs to which note. There are two ways,
under **Settings → Sync & export → Note metadata**:

- **In the file** (the default). Each file starts with a short block like
  this:

  ```yaml
  ---
  markleaf_id: 6f0c…
  created_at: 2026-10-28T09:00:00Z
  updated_at: 2026-10-28T09:05:00Z
  pinned: false
  archived: false
  body_sha256: 2a7e…
  ---
  ```

  Obsidian and most Markdown editors show it as properties or hide it. Keep
  the `markleaf_id` line: it is how Markleaf finds the note, and it is why
  renaming the file on the computer is safe in this mode. Other keys you add
  to the block are kept.
- **Beside the files.** The files hold only what you wrote, and the links
  live in hidden `.markleaf-index-…` files in the same folder. Sync those
  hidden files too; Syncthing does by default. The trade-off: with no id in
  the file, a renamed file is matched to its note by its content. A plain
  rename keeps the same note, but a file renamed *and* edited before
  Markleaf syncs, or renamed while you are editing that note on the phone,
  is read in as a new note. Rename, let Markleaf sync, then edit.

If you plan to rename files on the computer, use **In the file**.

## 6. Quick reference

| On the computer you… | What happens in Markleaf |
|---|---|
| Edit a file | The note updates the next time Markleaf syncs |
| Create a new `.md` file in the folder | It becomes a new note |
| Rename a file | Same note, and with **Keep file names** on the name stays. Index mode only: if you also edited it before Markleaf synced, it arrives as a new note |
| Delete a file | Nothing; the note stays, and its file is written again on the next edit |
| Edit a note in Markleaf and the same file on the computer | The phone keeps its version; the other arrives as a copy |

Something not working as described? Open an issue at
<https://github.com/jeiel85/markleaf-android/issues>.
