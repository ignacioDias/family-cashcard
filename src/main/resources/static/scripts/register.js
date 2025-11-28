const form = document.getElementById('registerForm');
const messageDiv = document.getElementById('message');
const submitBtn = document.getElementById('submitBtn');

form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    messageDiv.className = 'message';
    messageDiv.style.display = 'none';

    if (password !== confirmPassword) {
        showMessage('Passwords do not match', 'error');
        return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = 'Creating Account...';

    try {
        const response = await fetch('/users/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ username, password })
        });

        if (response.status === 201) {
            showMessage('Account created successfully! Redirecting to login...', 'success');
            setTimeout(() => {
                window.location.href = '/login.html';
            }, 2000);
        } else if (response.status === 409) {
            showMessage('Username already exists. Please choose another.', 'error');
            submitBtn.disabled = false;
            submitBtn.textContent = 'Create Account';
        } else if (response.status === 400) {
            showMessage('Invalid input. Please check your username and password.', 'error');
            submitBtn.disabled = false;
            submitBtn.textContent = 'Create Account';
        } else {
            showMessage('Registration failed. Please try again.', 'error');
            submitBtn.disabled = false;
            submitBtn.textContent = 'Create Account';
        }
    } catch (error) {
        showMessage('Network error. Please check your connection and try again.', 'error');
        submitBtn.disabled = false;
        submitBtn.textContent = 'Create Account';
    }
});

function showMessage(text, type) {
    messageDiv.textContent = text;
    messageDiv.className = `message ${type}`;
    messageDiv.style.display = 'block';
}