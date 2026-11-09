/**
 * Submits a comment textarea's form when Enter is pressed.
 * Shift+Enter still inserts a newline.
 */
document.querySelectorAll('textarea.js-comment-textarea').forEach(textarea => {
    textarea.addEventListener('keydown', e => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            textarea.closest('form').requestSubmit();
        }
    });
});

// Autofocus the textarea when a reply/edit form finishes expanding.
document.addEventListener('shown.bs.collapse', e => {
    const textarea = e.target.querySelector('textarea.js-comment-textarea');
    if (textarea) {
        textarea.focus();
    }
});
