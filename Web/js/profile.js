document.addEventListener('DOMContentLoaded', () => {
    const path = window.location.pathname;
    const rawId = path.split('/profile/')[1];

    if (!rawId) {
        window.location.href = '/';
        return;
    }

    const decodedId = decodeURIComponent(rawId);
    let gameName, tagLine;

    if (decodedId.includes('-')) {
        [gameName, tagLine] = decodedId.split('-');
    } else if (decodedId.includes('#')) {
        [gameName, tagLine] = decodedId.split('#');
    } else {
        gameName = decodedId;
        tagLine = 'NA1';
    }

    loadProfile(gameName, tagLine);
    setupInfiniteScroll(gameName, tagLine);
});

async function loadProfile(gameName, tagLine) {
    try {
        const response = await fetch(`https://api.axioscomputers.com/api/v1/profile/${gameName}/${tagLine}`);

        if (!response.ok) throw new Error('Profile not found');

        const data = await response.json();
        renderHeader(data);
        loadMatches(gameName, tagLine, 0);
    } catch (error) {
        document.getElementById('summonerName').innerText = 'Profile Not Found';
        document.getElementById('summonerTag').innerText = '';
    }
}

function renderHeader(data) {
    document.getElementById('summonerName').innerText = data.gameName;
    document.getElementById('summonerTag').innerText = `#${data.tagLine}`;
    document.getElementById('summonerLevel').innerText = data.summonerLevel;

    const iconImg = document.getElementById('profileIcon');
    iconImg.src = `https://ddragon.leagueoflegends.com/cdn/16.1.1/img/profileicon/${data.profileIconId}.png`;

    if (data.rankTier) {
        const rankEmblem = document.getElementById('rankEmblem');
        rankEmblem.src = `/assets/ranks/${data.rankTier.toLowerCase()}.png`;
        rankEmblem.style.display = 'block';

        document.getElementById('rankTier').innerText = `${data.rankTier} ${data.rankDivision}`;
        document.getElementById('rankLP').innerText = `${data.leaguePoints} LP`;
    }
}

let isLoadingMatches = false;
let currentOffset = 0;
let hasMoreMatches = true;

async function loadMatches(gameName, tagLine, offset) {
    if (isLoadingMatches || !hasMoreMatches) return;

    isLoadingMatches = true;
    const loader = document.getElementById('scrollLoader');
    loader.style.display = 'block';

    try {
        const response = await fetch(`https://api.axioscomputers.com/api/v1/profile/${gameName}/${tagLine}/matches?start=${offset}&count=10`);
        const matches = await response.json();

        if (matches.length < 10) {
            hasMoreMatches = false;
            loader.style.display = 'none';
        }

        const matchList = document.getElementById('matchList');
        matches.forEach(match => {
            matchList.appendChild(createMatchCard(match));
        });

        currentOffset += 10;
    } catch (error) {
        console.error('Failed to load matches');
        hasMoreMatches = false;
        loader.style.display = 'none';
    } finally {
        isLoadingMatches = false;
    }
}

function createMatchCard(match) {
    const card = document.createElement('div');
    card.className = `match-card ${match.win ? 'win' : 'loss'}`;

    const kda = ((match.kills + match.assists) / Math.max(1, match.deaths)).toFixed(2);

    card.innerHTML = `
        <div class="match-info-main">
            <div class="result-indicator">
                <div class="game-result">${match.win ? 'Victory' : 'Defeat'}</div>
                <div class="stat-detail">${match.gameMode}</div>
                <div class="stat-detail">${match.gameDuration}</div>
            </div>

            <img class="champ-circle" src="https://ddragon.leagueoflegends.com/cdn/16.1.1/img/champion/${match.championName}.png" alt="${match.championName}">
            
            <div class="kda-stats">
                <div class="kda-text">${match.kills} / <span style="color: #dc3545;">${match.deaths}</span> / ${match.assists}</div>
                <div class="stat-detail">${kda} KDA</div>
            </div>
        </div>
        
        <div class="match-details-right">
            <div class="stat-detail">CS ${match.totalMinionsKilled} (${match.csPerMin}/m)</div>
        </div>
    `;
    return card;
}

function setupInfiniteScroll(gameName, tagLine) {
    const loader = document.getElementById('scrollLoader');

    const observer = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting) {
            loadMatches(gameName, tagLine, currentOffset);
        }
    }, { rootMargin: '100px' });

    observer.observe(loader);
}