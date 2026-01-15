async function loadComponents() {
    const components = {
        'global-nav': '/components/navbar.html',
        'side-sidebar': '/components/sidebar.html',
        'ad-footer': '/components/ad-footer.html',
        'region-selector-container': '/components/region-selector.html',
        'info-footer': '/components/info-footer.html'
    };

    const promises = Object.entries(components).map(async ([className, filePath]) => {
        const element = document.querySelector(`.${className}`);
        if (element) {
            try {
                const response = await fetch(filePath);
                if (response.ok) {
                    const html = await response.text();
                    element.innerHTML = html;
                }
            } catch (err) {
                console.error(err);
            }
        }
    });

    await Promise.all(promises);
    updateAuthUI();

    const isHomePage = window.location.pathname === '/' || window.location.pathname === '/index.html';
    if (isHomePage) {
        document.body.classList.add('home-page');
        setupSearch('homeSearchInput', 'homeSearchDropdown');
    } else {
        setupSearch('globalSearchInput', 'searchDropdown', 'globalSearchBtn');
    }

    fetchUserProfile();
}

function updateAuthUI() {
    const token = localStorage.getItem('token');
    const path = window.location.pathname;
    const loggedOutLinks = document.getElementById('loggedOutLinks');
    const loggedInLinks = document.getElementById('loggedInLinks');
    const isAuthPage = path.includes('login') || path.includes('signup');

    if (isAuthPage) {
        if (loggedOutLinks) loggedOutLinks.style.display = 'none';
        if (loggedInLinks) loggedInLinks.style.display = 'none';
    } else {
        if (token) {
            if (loggedOutLinks) loggedOutLinks.style.display = 'none';
            if (loggedInLinks) loggedInLinks.style.display = 'flex';
        } else {
            if (loggedOutLinks) loggedOutLinks.style.display = 'flex';
            if (loggedInLinks) loggedInLinks.style.display = 'none';
        }
    }

    const authZone = document.getElementById('sidebarAuthZone');
    if (authZone && token) {
        authZone.innerHTML = `
            <div class="sidebar-item" id="settingsSidebarItem" title="Settings">
                <span class="icon">⚙️</span>
                <span class="label">Settings</span>
            </div>
            <div class="sidebar-item" id="logoutBtn" title="Logout">
                <span class="icon">🚪</span>
                <span class="label">Logout</span>
            </div>
        `;

        const settingsItem = document.getElementById('settingsSidebarItem');
        if (settingsItem) {
            settingsItem.addEventListener('click', () => {
                window.location.href = '/settings';
            });
        }

        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => {
                localStorage.removeItem('token');
                window.location.href = '/';
            });
        }
    } else if (authZone) {
        authZone.innerHTML = '';
    }
}


const signupForm = document.getElementById('signupForm');
if (signupForm) {
    signupForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('username').value;
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const messageElement = document.getElementById('message');

        try {
            const response = await fetch('https://api.axioscomputers.com/api/v1/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, email, password })
            });
            const data = await response.json();
            messageElement.innerText = response.ok ? "Check your email to verify!" : (data.message || "Registration failed.");
            messageElement.style.color = response.ok ? "lightgreen" : "red";
        } catch (error) {
            messageElement.innerText = "Error connecting to server.";
        }
    });
}

const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const identifier = document.getElementById('loginIdentifier').value;
        const password = document.getElementById('loginPassword').value;
        const messageElement = document.getElementById('loginMessage');

        try {
            const response = await fetch('https://api.axioscomputers.com/api/v1/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ identifier, password })
            });

            const responseText = await response.text();
            let data;
            try {
                data = JSON.parse(responseText);
            } catch (err) {
                data = { message: responseText };
            }

            if (response.ok) {
                localStorage.setItem('token', data.token);
                messageElement.style.color = "lightgreen";
                messageElement.innerText = "Login Successful!";
                setTimeout(() => { window.location.href = '/'; }, 1000);
            } else {
                messageElement.style.color = "red";
                messageElement.innerText = data.message || data.error || responseText || "Login failed.";
            }
        } catch (error) {
            messageElement.innerText = "Error connecting to server.";
        }
    });
}

async function fetchUserProfile() {
    const token = localStorage.getItem('token');
    if (!token) return;

    try {
        const response = await fetch('https://api.axioscomputers.com/api/v1/user/me', {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            const user = await response.json();

            if (user.riotLinked) {
                const profileBtn = document.getElementById('userProfileBtn');
                const nameSpan = document.getElementById('pGameName');
                const tagSpan = document.getElementById('pTagLine');

                if (profileBtn && nameSpan && tagSpan) {
                    nameSpan.innerText = user.gameName;
                    tagSpan.innerText = '#' + user.tagLine;
                    profileBtn.title = `${user.gameName} #${user.tagLine}`;
                    profileBtn.onclick = () => window.location.href = `/profile/${user.gameName}-${user.tagLine}`;
                    profileBtn.style.display = 'flex';
                }

                const vSection = document.querySelector('.verification-section');
                if (vSection) vSection.style.display = 'none';
            }

            const profileDiv = document.getElementById('userProfile');
            if (profileDiv) {
                const riotStatusHTML = user.riotLinked
                    ? `<span style="color: lightgreen;">✅ Linked (${user.gameName}#${user.tagLine})</span>`
                    : `<span style="color: orange;">❌ Not Linked</span>`;

                profileDiv.innerHTML = `
                    <div class="profile-details">
                        <p><strong>Email:</strong> ${user.email}</p>
                        <p><strong>Riot Account:</strong> ${riotStatusHTML}</p>
                    </div>
                `;
            }
        } else if (response.status === 401) {
            localStorage.removeItem('token');
        }
    } catch (error) {
        console.error("Failed to fetch profile");
    }
}

let verificationStage = "initiate";

const verifyBtn = document.getElementById('verifyBtn');
if (verifyBtn) {
    verifyBtn.addEventListener('click', async () => {
        if (verificationStage === "initiate") {
            await initiateVerification();
        } else if (verificationStage === "confirm") {
            await confirmVerification();
        }
    });
}

async function initiateVerification() {
    const gameName = document.getElementById('riotName').value;
    const tagLine = removeHash(document.getElementById('riotTag').value);
    const leagueRegion = document.getElementById('regionSelect').value;
    const messageElement = document.getElementById('verifyMessage');
    const iconContainer = document.getElementById('verifyIconContainer');
    const token = localStorage.getItem('token');

    try {
        messageElement.style.color = "white";
        messageElement.innerText = "Initiating...";

        const response = await fetch('https://api.axioscomputers.com/api/v1/auth/ign/initiate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ gameName, tagLine, leagueRegion })
        });

        const text = await response.text();

        if (response.ok) {
            const iconId = text.match(/\d+/)[0];
            const iconUrl = `https://ddragon.leagueoflegends.com/cdn/16.1.1/img/profileicon/${iconId}.png`;

            iconContainer.innerHTML = `
                <p style="color: orange; font-weight: bold;">Action Required:</p>
                <p>Change your League icon to this picture:</p>
                <img src="${iconUrl}" style="width:80px;height:80px;border-radius:8px;border:2px solid #4169e1;">
                <p style="font-size:0.85rem;color:#aaa;">Once changed, click confirm.</p>
            `;

            verifyBtn.innerText = "Confirm Icon Change";
            verificationStage = "confirm";
            messageElement.innerText = "";
        } else {
            messageElement.style.color = "red";
            messageElement.innerText = text || "Initiation failed.";
        }
    } catch (error) {
        messageElement.style.color = "red";
        messageElement.innerText = "Error connecting to server.";
    }
}

async function confirmVerification() {
    const token = localStorage.getItem('token');
    const messageElement = document.getElementById('verifyMessage');

    try {
        const response = await fetch('https://api.axioscomputers.com/api/v1/auth/ign/confirm', {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        let errorText = '';
        try {
            errorText = await response.text();
            if (!errorText) {
                const data = await response.json().catch(() => null);
                if (data?.message) errorText = data.message;
            }
        } catch {
            errorText = "Unknown backend error.";
        }

        if (response.ok) {
            messageElement.style.color = "lightgreen";
            messageElement.innerText = "Success! Account linked.";
            setTimeout(() => location.reload(), 1500);
        } else {
            messageElement.style.color = "red";
            messageElement.innerText = errorText || "Verification failed.";
        }
    } catch (networkError) {
        messageElement.style.color = "red";
        messageElement.innerText = networkError.message || "Network error connecting to server.";
    }
}


let searchTimeout = null;
let currentSelectionIndex = -1;

function removeHash(str) {
    if (str.startsWith('#')) { return str.substring(1); }
    return str;
}

function setupSearch(inputId, dropdownId, btnId = null) {
    const searchInput = document.getElementById(inputId);
    const dropdown = document.getElementById(dropdownId);
    const searchBtn = btnId ? document.getElementById(btnId) : null;

    if (!searchInput || !dropdown) return;

    searchInput.addEventListener('input', (e) => {
        const query = e.target.value.trim();
        clearTimeout(searchTimeout);

        if (query.length < 2) {
            closeDropdown(dropdownId);
            return;
        }

        searchTimeout = setTimeout(() => performSearch(query, dropdownId), 300);
    });

    searchInput.addEventListener('keydown', (e) => {
        const items = dropdown.querySelectorAll('.search-item');

        if (e.key === 'ArrowDown') {
            if (items.length === 0) return;
            e.preventDefault();
            currentSelectionIndex++;
            if (currentSelectionIndex >= items.length) currentSelectionIndex = 0;
            updateSelection(items);
        } else if (e.key === 'ArrowUp') {
            if (items.length === 0) return;
            e.preventDefault();
            currentSelectionIndex--;
            if (currentSelectionIndex < 0) currentSelectionIndex = items.length - 1;
            updateSelection(items);
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (currentSelectionIndex > -1 && items[currentSelectionIndex]) {
                items[currentSelectionIndex].click();
            } else {
                performRedirect(searchInput.value, true);
            }
        } else if (e.key === 'Escape') {
            closeDropdown(dropdownId);
        }
    });

    document.addEventListener('click', (e) => {
        if (!searchInput.contains(e.target) && !dropdown.contains(e.target)) {
            closeDropdown(dropdownId);
        }
    });

    if (searchBtn) {
        searchBtn.addEventListener('click', () => {
            performRedirect(searchInput.value, true);
        });
    }
}

async function performSearch(query, dropdownId) {
    try {
        const response = await fetch(`https://api.axioscomputers.com/api/v1/search/autocomplete?query=${encodeURIComponent(query)}`);

        if (response.ok) {
            const results = await response.json();
            renderResults(results, dropdownId);
        } else {
            renderResults([
                { name: query, type: 'Summoner', url: `/profile/${query}`, image: '' },
                { name: "Ahri", type: "Champion", url: "/champion/ahri", image: "https://ddragon.leagueoflegends.com/cdn/14.1.1/img/champion/Ahri.png" }
            ], dropdownId);
        }
    } catch (error) {
        renderResults([
            { name: query, type: 'Summoner', url: `/profile/${query}`, image: '' },
            { name: "Ahri", type: "Champion", url: "/champion/ahri", image: "https://ddragon.leagueoflegends.com/cdn/14.1.1/img/champion/Ahri.png" }
        ], dropdownId);
    }
}

function performRedirect(query, forceRedirect = false) {
    if (forceRedirect) {
        if (query.includes('#')) {
            window.location.href = `/profile/${encodeURIComponent(query)}`;
        } else {
            window.location.href = `/search?q=${encodeURIComponent(query)}`;
        }
    }
}

function renderResults(results, dropdownId) {
    const dropdown = document.getElementById(dropdownId);
    if (!dropdown) return;

    dropdown.innerHTML = '';
    currentSelectionIndex = -1;

    if (!results || results.length === 0) {
        closeDropdown(dropdownId);
        return;
    }

    results.forEach(item => {
        const div = document.createElement('div');
        div.className = 'search-item';
        div.onclick = () => window.location.href = item.url;

        div.innerHTML = `
            <img src="${item.image || '/assets/default-icon.png'}" alt="">
            <div class="info">
                <span class="main-text">${item.name}</span>
                <span class="sub-text">${item.type}</span>
            </div>
        `;
        dropdown.appendChild(div);
    });

    dropdown.classList.add('open');
    if (dropdownId === 'searchDropdown') {
        document.querySelector('.nav-search')?.classList.add('open');
    }
}

function updateSelection(items) {
    items.forEach(item => item.classList.remove('selected'));
    if (currentSelectionIndex > -1 && items[currentSelectionIndex]) {
        items[currentSelectionIndex].classList.add('selected');
    }
}

function closeDropdown(dropdownId) {
    const dropdown = document.getElementById(dropdownId);
    if (dropdown) {
        dropdown.classList.remove('open');
        dropdown.innerHTML = '';
    }

    if (dropdownId === 'searchDropdown') {
        document.querySelector('.nav-search')?.classList.remove('open');
    }
    currentSelectionIndex = -1;
}

window.addEventListener('DOMContentLoaded', () => {
    loadComponents();
});