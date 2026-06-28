document.addEventListener('DOMContentLoaded', () => {
    const translateForm = document.getElementById('translateForm');
    const lyricsTextarea = document.getElementById('lyrics');
    const lyricsError = document.getElementById('lyricsError');
    const loadingOverlay = document.getElementById('loadingOverlay');
    const submitBtn = document.getElementById('submitBtn');
    const submitText = document.getElementById('submitText');
    const submitSpinner = document.getElementById('submitSpinner');
    const loadingStatusText = document.getElementById('loadingStatusText');

    // List of messages to rotate through during the loading process
    const loadingMessages = [
        "Menghubungi AI penerjemah...",
        "Menganalisis melodi dan suasana lirik...",
        "Menerjemahkan baris per baris ke Bahasa Indonesia...",
        "Mengekstrak makna mendalam lagu...",
        "Merangkum narasi cerita lagu...",
        "Menyusun visualisasi hasil terjemahan...",
        "Hampir selesai, memfinalisasi data..."
    ];

    if (translateForm) {
        translateForm.addEventListener('submit', (e) => {
            const lyricsVal = lyricsTextarea.value.trim();

            // Client-side validation
            if (!lyricsVal) {
                e.preventDefault();
                showError("Lirik lagu tidak boleh kosong.");
                return;
            }

            if (lyricsVal.length < 10) {
                e.preventDefault();
                showError("Lirik lagu terlalu pendek. Masukkan minimal 10 karakter.");
                return;
            }

            if (lyricsVal.length > 10000) {
                e.preventDefault();
                showError("Lirik lagu terlalu panjang. Maksimal 10.000 karakter.");
                return;
            }

            // Clear errors and show loading overlay
            hideError();
            showLoadingState();
        });
    }

    function showError(message) {
        if (lyricsError) {
            lyricsError.textContent = message;
            lyricsError.classList.remove('hidden');
            lyricsTextarea.classList.add('border-red-500', 'focus:border-red-500');
            lyricsTextarea.classList.remove('border-gray-700', 'focus:border-indigo-500');
        }
    }

    function hideError() {
        if (lyricsError) {
            lyricsError.classList.add('hidden');
            lyricsTextarea.classList.remove('border-red-500', 'focus:border-red-500');
            lyricsTextarea.classList.add('border-gray-700', 'focus:border-indigo-500');
        }
    }

    function showLoadingState() {
        // Show overlay
        if (loadingOverlay) {
            loadingOverlay.classList.remove('hidden');
            loadingOverlay.classList.add('flex');
            // Disable scroll on body
            document.body.classList.add('overflow-hidden');
        }

        // Disable button and show spinner
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.classList.add('opacity-75', 'cursor-not-allowed');
        }
        if (submitText) {
            submitText.textContent = "Sedang Menerjemahkan...";
        }
        if (submitSpinner) {
            submitSpinner.classList.remove('hidden');
        }

        // Rotate status messages
        let messageIndex = 0;
        if (loadingStatusText) {
            loadingStatusText.textContent = loadingMessages[0];
            const intervalId = setInterval(() => {
                messageIndex = (messageIndex + 1) % loadingMessages.length;
                
                // Fade out transition
                loadingStatusText.style.opacity = 0;
                setTimeout(() => {
                    loadingStatusText.textContent = loadingMessages[messageIndex];
                    loadingStatusText.style.opacity = 1;
                }, 300);

            }, 3000);

            // Store interval ID in window in case we need to clear it (though page redirects)
            window.loadingMessageInterval = intervalId;
        }
    }
});
