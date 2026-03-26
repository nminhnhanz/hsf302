function debounce(fn, wait) {
    let t;
    return function (...args) {
        clearTimeout(t);
        t = setTimeout(() => fn.apply(this, args), wait);
    };
}

async function fetchJson(url) {
    try {
        const res = await fetch(url, { headers: { Accept: "application/json" } });
        if (!res.ok) return [];
        return await res.json();
    } catch (e) {
        console.error("Category/Tag lookup failed", e);
        return [];
    }
}

function showSuggestions(box, items, onPick) {
    box.innerHTML = "";
    if (!items || items.length === 0) {
        box.classList.add("d-none");
        return;
    }

    for (const it of items) {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.textContent = it.label || it.name;
        if (it.hint) {
            btn.title = it.hint;
        }
        btn.addEventListener("mousedown", (e) => {
            e.preventDefault();
            onPick(it);
        });
        box.appendChild(btn);
    }
    box.classList.remove("d-none");
}

function hideSuggestions(box) {
    box.classList.add("d-none");
    box.innerHTML = "";
}

document.addEventListener("DOMContentLoaded", () => {
    const categoryInput = document.getElementById("categoryInput");
    const categoryHidden = document.getElementById("categoryHidden");
    const categorySuggestions = document.getElementById("categorySuggestions");
    const categorySelected = document.getElementById("categorySelected");
    const newCategoryNameHidden = document.getElementById("newCategoryNameHidden");
    const initialCategoryName = document.getElementById("initialCategoryName")?.value || "";

    const tagInput = document.getElementById("tagInput");
    const tagsHidden = document.getElementById("tagsHidden");
    const tagSuggestions = document.getElementById("tagSuggestions");
    const tagsSelected = document.getElementById("tagsSelected");
    const newTagNamesHidden = document.getElementById("newTagNamesHidden");

    let cachedCategories = [];
    let selectedTags = [];

    function renderCategoryChip(id, name, isNew = false) {
        if (!categorySelected) return;
        categorySelected.innerHTML = "";
        if (!id || !name) return;

        const chip = document.createElement("span");
        chip.className = "chip";
        chip.innerHTML = `<span>${name}${isNew ? " (new)" : ""}</span>`;

        const x = document.createElement("button");
        x.type = "button";
        x.className = "x";
        x.textContent = "x";
        x.addEventListener("click", () => {
            categoryHidden.value = "";
            categoryHidden.dispatchEvent(new Event("change"));
            if (newCategoryNameHidden) newCategoryNameHidden.value = "";
            if (newCategoryNameHidden) newCategoryNameHidden.dispatchEvent(new Event("change"));
            categoryInput.value = "";
            renderCategoryChip("", "");
        });

        chip.appendChild(x);
        categorySelected.appendChild(chip);
    }

    function syncTagsHidden() {
        if (!tagsHidden) return;
        tagsHidden.value = selectedTags.filter((t) => !t.isNew).map((t) => t.id).join(",");
        if (newTagNamesHidden) {
            newTagNamesHidden.value = selectedTags.filter((t) => t.isNew).map((t) => t.name).join("||");
        }
    }

    function renderTagChips() {
        if (!tagsSelected) return;
        tagsSelected.innerHTML = "";

        for (const t of selectedTags) {
            const chip = document.createElement("span");
            chip.className = "chip";
            chip.innerHTML = `<span>${t.name}${t.isNew ? " (new)" : ""}</span>`;

            const x = document.createElement("button");
            x.type = "button";
            x.className = "x";
            x.textContent = "x";
            x.addEventListener("click", () => {
                selectedTags = selectedTags.filter((it) => String(it.id) !== String(t.id) || !!it.isNew !== !!t.isNew);
                syncTagsHidden();
                renderTagChips();
            });

            chip.appendChild(x);
            tagsSelected.appendChild(chip);
        }
    }

    async function ensureAllCategories() {
        if (cachedCategories.length > 0) return cachedCategories;
        cachedCategories = await fetchJson("/api/admin/categories?q=&limit=500");
        return cachedCategories;
    }

    const runCategoryLookup = debounce(async () => {
        const q = (categoryInput?.value || "").trim().toLowerCase();
        const all = await ensureAllCategories();
        const filtered = q.length < 1 ? all : all.filter((c) => (c.name || "").toLowerCase().includes(q));

        const items = [...filtered];
        const exact = q.length > 0 && all.some((c) => (c.name || "").toLowerCase() === q);
        if (q.length > 0 && !exact) {
            items.unshift({
                id: "new-category",
                name: q,
                label: `+ Create category "${q}"`,
                hint: "Ban chua co category nay, xac nhan tao moi chung.",
                isCreate: true
            });
        }

        showSuggestions(categorySuggestions, items, (picked) => {
            if (picked.isCreate) {
                categoryHidden.value = "";
                categoryHidden.dispatchEvent(new Event("change"));
                if (newCategoryNameHidden) newCategoryNameHidden.value = picked.name;
                if (newCategoryNameHidden) newCategoryNameHidden.dispatchEvent(new Event("change"));
                renderCategoryChip("pending", picked.name, true);
            } else {
                categoryHidden.value = picked.id;
                categoryHidden.dispatchEvent(new Event("change"));
                if (newCategoryNameHidden) newCategoryNameHidden.value = "";
                if (newCategoryNameHidden) newCategoryNameHidden.dispatchEvent(new Event("change"));
                renderCategoryChip(picked.id, picked.name, false);
            }
            categoryInput.value = "";
            hideSuggestions(categorySuggestions);
        });
    }, 250);

    const runTagLookup = debounce(async () => {
        const q = (tagInput?.value || "").trim();
        if (q.length < 1) {
            hideSuggestions(tagSuggestions);
            return;
        }

        const items = await fetchJson(`/api/admin/tags?q=${encodeURIComponent(q)}&limit=20`);
        const normalizedQ = q.toLowerCase();
        const exact = items.some((t) => (t.name || "").toLowerCase() === normalizedQ);
        const suggestions = [...items];
        if (!exact) {
            suggestions.unshift({
                id: `new-${normalizedQ}`,
                name: q,
                label: `+ Create tag "${q}"`,
                hint: "Ban chua co tag nay, xac nhan tao moi chung.",
                isCreate: true
            });
        }

        showSuggestions(tagSuggestions, suggestions, (picked) => {
            const existed = selectedTags.find((t) =>
                picked.isCreate
                    ? t.isNew && t.name.toLowerCase() === picked.name.toLowerCase()
                    : String(t.id) === String(picked.id)
            );
            if (!existed && selectedTags.length < 5) {
                selectedTags.push({
                    id: picked.id,
                    name: picked.name,
                    isNew: !!picked.isCreate
                });
                syncTagsHidden();
                renderTagChips();
            }
            tagInput.value = "";
            hideSuggestions(tagSuggestions);
        });
    }, 250);

    const initialTagSpans = document.querySelectorAll("#initialTagsData span");
    initialTagSpans.forEach((s) => {
        selectedTags.push({ id: s.getAttribute("data-id"), name: s.getAttribute("data-name"), isNew: false });
    });
    syncTagsHidden();
    renderTagChips();

    if (categoryHidden?.value && initialCategoryName) {
        renderCategoryChip(categoryHidden.value, initialCategoryName, false);
    }

    if (categoryInput) {
        categoryInput.addEventListener("keydown", (e) => {
            if (e.key === "Enter") e.preventDefault();
        });
        categoryInput.addEventListener("focus", async () => {
            await ensureAllCategories();
            runCategoryLookup();
        });
        categoryInput.addEventListener("input", runCategoryLookup);
        categoryInput.addEventListener("blur", () => hideSuggestions(categorySuggestions));
    }

    if (tagInput) {
        tagInput.addEventListener("keydown", (e) => {
            if (e.key === "Enter") e.preventDefault();
        });
        tagInput.addEventListener("input", runTagLookup);
        tagInput.addEventListener("blur", () => hideSuggestions(tagSuggestions));
    }
});




