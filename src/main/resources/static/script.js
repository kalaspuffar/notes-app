document.addEventListener("DOMContentLoaded", () => {
  const addBtn = document.getElementById("addBtn");
  const noteTextArea = document.getElementById("noteText");
  const notesList = document.getElementById("notesList");

  // Fetch existing notes
  fetch('/api/notes')
    .then(res => res.json())
    .then(data => {
      data.forEach(n => appendNote(n));
    })
    .catch(console.error);

  // Add button - create new note
  addBtn.addEventListener("click", () => {
    const text = noteTextArea.value.trim();
    if (!text) return;

    fetch('/api/notes', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text })
    })
    .then(res => res.json())
    .then(newNote => {
      appendNote(newNote);
      noteTextArea.value = '';
    })
    .catch(console.error);
  });

  function appendNote(note) {
    const li = document.createElement('li');
    li.textContent = `#${note.id}: ${note.text} (votes: ${note.votes})`;
    notesList.appendChild(li);
  }
});
