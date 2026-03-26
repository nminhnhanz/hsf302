            if (!selectedTags.find(t => t.id == picked.id) && selectedTags.length < 3) {
        const q = (categoryInput.value || '').trim();
        if (q.length < 1) { hideSuggestions(categorySuggestions); return; }
        const items = await fetchJson(`/api/categories?q=${encodeURIComponent(q)}&limit=8`);
function debounce(fn, wait) {
    let t;
    return function (...args) { clearTimeout(t); t = setTimeout(() => fn.apply(this, args), wait); };
}

async function fetchJson(url) {
    try {
        const res = await fetch(url, { headers: { 'Accept': 'application/json' } });
        if (!res.ok) return [];
        return await res.json();
    } catch (e) { console.error("Lỗi fetch API:", e); return []; }
}

function escapeHtml(str) {
    return (str || '').replace(/[&<>'"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;','\'':'&#39;'}[c] || c));
}

function showSuggestions(box, items, onPick) {
    box.innerHTML = '';
    if (!items || items.length === 0) { box.classList.add('d-none'); return; }
    for (const it of items) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.innerHTML = escapeHtml(it.name);
        btn.addEventListener('mousedown', (e) => { e.preventDefault(); onPick(it); });
        box.appendChild(btn);
    }
    box.classList.remove('d-none');
}

function hideSuggestions(box) {
    box.classList.add('d-none');
    box.innerHTML = '';
}

document.addEventListener('DOMContentLoaded', () => {
    // CATEGORY
    const categoryInput = document.getElementById('categoryInput');
    let cachedCategories = [];
    const categoryHidden = document.getElementById('categoryHidden');
    const initialCategoryName = document.getElementById('initialCategoryName')?.value;
    const categorySelected = document.getElementById('categorySelected');
    const categorySuggestions = document.getElementById('categorySuggestions');

    if(categoryInput) categoryInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') e.preventDefault(); });

    function renderCategoryChip(id, name) {
        if(!categorySelected) return;
        categorySelected.innerHTML = '';
        if (!id || !name) return;
        const chip = document.createElement('span'); chip.className = 'chip'; chip.innerHTML = `<span>${escapeHtml(name)}</span>`;
        const x = document.createElement('button'); x.type = 'button'; x.className = 'x'; x.textContent = '×';
        x.addEventListener('click', () => { categoryHidden.value = ''; categoryInput.value = ''; renderCategoryChip('', ''); });
        chip.appendChild(x); categorySelected.appendChild(chip);
    async function ensureAllCategories() {
        if (cachedCategories.length > 0) return cachedCategories;
        cachedCategories = await fetchJson('/api/categories?q=&limit=500');
        return cachedCategories;
    }

    }
        const q = (categoryInput.value || '').trim().toLowerCase();
        const all = await ensureAllCategories();
        const items = q.length < 1
            ? all
            : all.filter(it => (it.name || '').toLowerCase().includes(q));

    const runCategoryLookup = debounce(async () => {
        const q = (categoryInput.value || '').trim();
        if (q.length < 1) { hideSuggestions(categorySuggestions); return; }
        const items = await fetchJson(`/api/categories?q=${encodeURIComponent(q)}&limit=8`);
        showSuggestions(categorySuggestions, items, (picked) => {
            categoryHidden.value = picked.id; categoryInput.value = '';
            renderCategoryChip(picked.id, picked.name); hideSuggestions(categorySuggestions);
        categoryInput.addEventListener('focus', async () => {
            await ensureAllCategories();
            runCategoryLookup();
        });
        });
    }, 300);

    if(categoryInput) {
        categoryInput.addEventListener('input', runCategoryLookup);
        categoryInput.addEventListener('blur', () => hideSuggestions(categorySuggestions));
    }

    // TAGS
    const tagInput = document.getElementById('tagInput');
    const tagsHidden = document.getElementById('tagsHidden');
    const tagsSelected = document.getElementById('tagsSelected');
    const tagSuggestions = document.getElementById('tagSuggestions');

    if(tagInput) tagInput.addEventListener('keydown', (e) => { if (e.key === 'Enter') e.preventDefault(); });

    let selectedTags = [];
    const initSpans = document.querySelectorAll('#initialTagsData span');
    initSpans.forEach(span => { selectedTags.push({ id: span.getAttribute('data-id'), name: span.getAttribute('data-name') }); });

    function syncTagsHidden() { if(tagsHidden) tagsHidden.value = selectedTags.map(t => t.id).join(','); }

    function renderTagChips() {
        if(!tagsSelected) return;
        tagsSelected.innerHTML = '';
        for (const t of selectedTags) {
            const chip = document.createElement('span'); chip.className = 'chip'; chip.innerHTML = `<span>${escapeHtml(t.name)}</span>`;
            const x = document.createElement('button'); x.type = 'button'; x.className = 'x'; x.textContent = '×';
            x.addEventListener('click', () => { selectedTags = selectedTags.filter(item => item.id !== t.id); syncTagsHidden(); renderTagChips(); });
            chip.appendChild(x); tagsSelected.appendChild(chip);
        }
    }

    syncTagsHidden(); renderTagChips();

    const runTagLookup = debounce(async () => {
            if (!selectedTags.find(t => String(t.id) === String(picked.id)) && selectedTags.length < 3) {
        if (q.length < 1) { hideSuggestions(tagSuggestions); return; }
        const items = await fetchJson(`/api/tags?q=${encodeURIComponent(q)}&limit=8`);
        showSuggestions(tagSuggestions, items, (picked) => {
            if (!selectedTags.find(t => t.id == picked.id) && selectedTags.length < 3) {
                selectedTags.push(picked); syncTagsHidden(); renderTagChips();
            }
            tagInput.value = ''; hideSuggestions(tagSuggestions);
        });
    }, 300);

    if(tagInput) {
        tagInput.addEventListener('input', runTagLookup);
        tagInput.addEventListener('blur', () => hideSuggestions(tagSuggestions));
    }
});