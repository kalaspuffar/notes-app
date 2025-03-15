document.addEventListener("DOMContentLoaded", () => {
  const addBtn = document.getElementById("addBtn");
  const noteTextArea = document.getElementById("noteText");
  const notesList = document.getElementById("notesList");

  // Fetch existing notes on page load
  fetch('/api/notes')
    .then(res => res.json())
    .then(notes => {
      notesList.innerHTML = "";
      notes.forEach(n => {
        appendNote(n);
      });
    })
    .catch(console.error);

  // On "Add Note" click -> POST /api/notes
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
      noteTextArea.value = '';
      appendNote(newNote);
    })
    .catch(console.error);
  });

  function appendNote(note) {
    const li = document.createElement('li');
    li.textContent = `#${note.id}: ${note.text} (votes: ${note.votes})`;

    // Create an upvote button
    const upBtn = document.createElement('button');
    upBtn.textContent = 'Upvote';
    upBtn.style.marginLeft = '10px';
    upBtn.addEventListener('click', () => {
      voteOnNote(note.id, 'up');
    });

    // Create a downvote button
    const downBtn = document.createElement('button');
    downBtn.textContent = 'Downvote';
    downBtn.style.marginLeft = '5px';
    downBtn.addEventListener('click', () => {
      voteOnNote(note.id, 'down');
    });

    // Create a delete button
    const deleteBtn = document.createElement('button');
    deleteBtn.textContent = 'Delete';
    deleteBtn.style.marginLeft = '5px';
    deleteBtn.addEventListener('click', () => {
      deleteNote(note.id);
    });

    li.appendChild(upBtn);
    li.appendChild(downBtn);
    li.appendChild(deleteBtn);

    notesList.appendChild(li);
  }

  function voteOnNote(noteId, voteType) {
    // POST /api/notes/{id}/vote?type=up or ?type=down
    fetch(`/api/notes/${noteId}/vote?type=${voteType}`, {
      method: 'POST'
    })
    .then(res => {
      if (!res.ok) throw new Error('Vote failed');
      return res.json();
    })
    .then(updatedNote => {
      // We can either re-fetch all notes, or just update the UI for this one
      refreshNotes();
    })
    .catch(console.error);
  }

  function deleteNote(noteId) {
    // DELETE /api/notes/{id}
    fetch(`/api/notes/${noteId}`, {
      method: 'DELETE'
    })
    .then(res => {
      if (!res.ok) throw new Error('delete failed');
      refreshNotes();
    })
    .catch(console.error);
  }



  function refreshNotes() {
    fetch('/api/notes')
      .then(res => res.json())
      .then(notes => {
        notesList.innerHTML = "";
        notes.forEach(n => appendNote(n));
      })
      .catch(console.error);
  }
});
