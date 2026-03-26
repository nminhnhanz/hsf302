function debounce(fn, wait) {
    let t;
    return function (...args) {
        clearTimeout(t);
        t = setTimeout(() => fn.apply(this, args), wait);
    };
}

async function fetchJson(url) {
    try {
        const res = await fetch(url, { headers: { Accept: 'application/json' } });
        if (!res.ok) return [];
        return await res.json();
    } catch (e) {
        console.error('Lookup failed', e);
        return [];
    }
}

function escapeHtml(str) {
    return (str || '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] || c));
}

function showSuggestions(box, items, onPick) {
    box.innerHTML = '';
    if (!items || items.length === 0) {
        box.classList.add('d-none');
        return;
    }
    items.forEach(it => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.innerHTML = escapeHtml(it.name);
        btn.addEventListener('mousedown', e => {
            e.preventDefault();
            onPick(it);
        });
        box.appendChild(btn);
    });
    box.classList.remove('d-none');
}

function hideSuggestions(box) {
    box.classList.add('d-none');
    box.innerHTML = '';
}

document.addEventListener('DOMContentLoaded', () => {
    const MAX_FILTER_TAGS = 3;
    const categoryInput = document.getElementById('categoryInput');
    const categoryHidden = document.getElementById('categoryHidden');
    const initialCategoryName = document.getElementById('initialCategoryName')?.value;
    const categorySelected = document.getElementById('categorySelected');
    const categorySuggestions = document.getElementById('categorySuggestions');

    const tagInput = document.getElementById('tagInput');
    const tagsHidden = document.getElementById('tagsHidden');
    const tagsSelected = document.getElementById('tagsSelected');
    const tagSuggestions = document.getElementById('tagSuggestions');

    let selectedTags = [];
    let cachedCategories = [];

    function renderCategoryChip(id, name) {
        if (!categorySelected) return;
        categorySelected.innerHTML = '';
        if (!id || !name) return;
        const chip = document.createElement('span');
        chip.className = 'chip';
        chip.innerHTML = `<span>${escapeHtml(name)}</span>`;
        const x = document.createElement('button');
        x.type = 'button';
        x.className = 'x';
        x.textContent = 'x';
        x.addEventListener('click', () => {
            categoryHidden.value = '';
            categoryInput.value = '';
            renderCategoryChip('', '');
        });
        chip.appendChild(x);
        categorySelected.appendChild(chip);
    }

    if (categoryHidden && categoryHidden.value && initialCategoryName) {
        renderCategoryChip(categoryHidden.value, initialCategoryName);
    }

    async function ensureAllCategories() {
        if (cachedCategories.length > 0) {
            return cachedCategories;
        }
        cachedCategories = await fetchJson('/api/admin/categories?q=&limit=500');
        return cachedCategories;
    }

    const runCategoryLookup = debounce(async () => {
        const q = (categoryInput.value || '').trim().toLowerCase();
        const all = await ensureAllCategories();
        const items = q.length < 1
            ? all
            : all.filter(it => (it.name || '').toLowerCase().includes(q));

        showSuggestions(categorySuggestions, items, picked => {
            categoryHidden.value = picked.id;
            categoryInput.value = '';
            renderCategoryChip(picked.id, picked.name);
            hideSuggestions(categorySuggestions);
        });
    }, 250);

    if (categoryInput) {
        categoryInput.addEventListener('keydown', e => { if (e.key === 'Enter') e.preventDefault(); });
        categoryInput.addEventListener('focus', async () => {
            await ensureAllCategories();
            runCategoryLookup();
        });
        categoryInput.addEventListener('input', runCategoryLookup);
        categoryInput.addEventListener('blur', () => hideSuggestions(categorySuggestions));
    }

    function syncTagsHidden() {
        if (!tagsHidden) return;
        tagsHidden.value = selectedTags.map(t => t.id).join(',');
    }

    function renderTagChips() {
        if (!tagsSelected) return;
        tagsSelected.innerHTML = '';
        selectedTags.forEach(t => {
            const chip = document.createElement('span');
            chip.className = 'chip';
            chip.innerHTML = `<span>${escapeHtml(t.name)}</span>`;
            const x = document.createElement('button');
            x.type = 'button';
            x.className = 'x';
            x.textContent = 'x';
            x.addEventListener('click', () => {
                selectedTags = selectedTags.filter(it => String(it.id) !== String(t.id));
                syncTagsHidden();
                renderTagChips();
            });
            chip.appendChild(x);
            tagsSelected.appendChild(chip);
        });
    }

    document.querySelectorAll('#initialTagsData span').forEach(span => {
        if (selectedTags.length < MAX_FILTER_TAGS) {
            selectedTags.push({ id: span.getAttribute('data-id'), name: span.getAttribute('data-name') });
        }
    });
    syncTagsHidden();
    renderTagChips();

    const runTagLookup = debounce(async () => {
        const q = (tagInput.value || '').trim();
        if (!q) {
            hideSuggestions(tagSuggestions);
            return;
        }
        const items = await fetchJson(`/api/admin/tags?q=${encodeURIComponent(q)}&limit=50`);
        showSuggestions(tagSuggestions, items, picked => {
            if (!selectedTags.find(t => String(t.id) === String(picked.id)) && selectedTags.length < MAX_FILTER_TAGS) {
                selectedTags.push(picked);
                syncTagsHidden();
                renderTagChips();
            }
            tagInput.value = '';
            hideSuggestions(tagSuggestions);
        });
    }, 250);

    if (tagInput) {
        tagInput.addEventListener('keydown', e => { if (e.key === 'Enter') e.preventDefault(); });
        tagInput.addEventListener('input', runTagLookup);
        tagInput.addEventListener('blur', () => hideSuggestions(tagSuggestions));
    }
});



