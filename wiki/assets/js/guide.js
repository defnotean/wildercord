(() => {
  'use strict';
  const menu = document.querySelector('#menu-button');
  const backdrop = document.querySelector('#nav-backdrop');
  const setMenu = open => {
    document.body.classList.toggle('nav-open', open);
    menu.setAttribute('aria-expanded', String(open));
    menu.setAttribute('aria-label', open ? 'Close guide navigation' : 'Open guide navigation');
    backdrop.hidden = !open;
  };
  menu.addEventListener('click', () => setMenu(menu.getAttribute('aria-expanded') !== 'true'));
  backdrop.addEventListener('click', () => setMenu(false));
  document.querySelectorAll('#guide-sidebar a').forEach(link => link.addEventListener('click', () => setMenu(false)));

  // Keep wide reference tables inside the article at small screen sizes.
  document.querySelectorAll('.article-content table').forEach(table => {
    const wrapper = document.createElement('div');
    wrapper.className = 'table-scroll';
    wrapper.tabIndex = 0;
    wrapper.setAttribute('role', 'region');
    wrapper.setAttribute('aria-label', 'Scrollable reference table');
    table.before(wrapper);
    wrapper.append(table);
  });
  document.querySelectorAll('.article-content img').forEach(image => {
    if (!image.closest('.home-hero') && !image.closest('.adventure-grid')) image.loading = 'lazy';
  });
  document.querySelectorAll('.article-content pre').forEach(pre => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'copy-code';
    button.textContent = 'Copy';
    button.setAttribute('aria-label', 'Copy code');
    button.addEventListener('click', async () => {
      try {
        await navigator.clipboard.writeText(pre.querySelector('code')?.textContent || '');
        button.textContent = 'Copied';
      } catch {
        button.textContent = 'Select code';
        const range = document.createRange();
        range.selectNodeContents(pre.querySelector('code') || pre);
        window.getSelection().removeAllRanges();
        window.getSelection().addRange(range);
      }
      setTimeout(() => { button.textContent = 'Copy'; }, 2000);
    });
    pre.append(button);
  });

  const outline = document.querySelector('#page-outline');
  if (outline) {
    const headings = [...document.querySelectorAll('.article-content h2')];
    headings.forEach((heading, index) => {
      if (!heading.id) heading.id = `section-${index + 1}`;
      const link = document.createElement('a');
      link.href = `#${heading.id}`;
      link.textContent = heading.textContent;
      outline.append(link);
    });
    const links = [...outline.querySelectorAll('a')];
    if (headings.length) {
      const observer = new IntersectionObserver(entries => {
        entries.forEach(entry => {
          if (!entry.isIntersecting) return;
          links.forEach(link => link.classList.toggle('active', link.hash === `#${entry.target.id}`));
        });
      }, {rootMargin: '-90px 0px -65% 0px'});
      headings.forEach(heading => observer.observe(heading));
    }
  }

  const dialog = document.querySelector('#search-dialog');
  const input = document.querySelector('#guide-search');
  const status = document.querySelector('#search-status');
  const results = document.querySelector('#search-results');
  let guideIndex;
  let indexRequest;
  const loadIndex = () => {
    if (!indexRequest) {
      indexRequest = fetch(`${document.body.dataset.baseurl}/assets/search-index.json`)
        .then(response => { if (!response.ok) throw new Error('Search unavailable'); return response.json(); })
        .then(data => { guideIndex = data; return data; })
        .catch(error => { indexRequest = undefined; throw error; });
    }
    return indexRequest;
  };
  const updateSearch = () => {
    results.replaceChildren();
    const query = input.value.trim().toLowerCase();
    if (!query) { status.textContent = 'Type to find a guide, rune or recipe.'; return; }
    if (!guideIndex) { status.textContent = 'Loading the guide…'; return; }
    const words = query.split(/\s+/);
    const matches = guideIndex.map(page => {
      const title = page.title.toLowerCase();
      const text = `${title} ${page.section} ${page.text}`.toLowerCase();
      const found = words.every(word => text.includes(word));
      return {page, score: found ? (title === query ? 100 : title.includes(query) ? 50 : 5) : 0};
    }).filter(entry => entry.score).sort((a,b) => b.score-a.score || a.page.title.localeCompare(b.page.title));
    status.textContent = matches.length ? `${matches.length} matching pages. Showing the first ${Math.min(12,matches.length)}.` : 'No matching pages. Try a rune name, element or shorter phrase.';
    matches.slice(0,12).forEach(({page}) => {
      const item = document.createElement('li');
      const anchor = document.createElement('a');
      anchor.href = page.url;
      const section = document.createElement('small');
      section.textContent = page.section;
      const title = document.createElement('strong');
      title.textContent = page.title;
      const snippet = document.createElement('p');
      const offset = Math.max(0, page.text.toLowerCase().indexOf(words[0]) - 35);
      snippet.textContent = (offset ? '…' : '') + page.text.slice(offset, offset+170).replace(/[#*`{}]/g,'') + '…';
      anchor.append(section,title,snippet);
      item.append(anchor);
      results.append(item);
    });
  };
  const openSearch = async () => {
    setMenu(false);
    if (!dialog.open) dialog.showModal();
    input.focus();
    try { await loadIndex(); updateSearch(); }
    catch { status.textContent = 'Search could not load. Browse the sidebar or try again.'; }
  };
  document.querySelector('#search-trigger').addEventListener('click', openSearch);
  input.addEventListener('input', updateSearch);
  input.addEventListener('keydown', event => {
    if (event.key === 'ArrowDown') { event.preventDefault(); results.querySelector('a')?.focus(); }
    if (event.key === 'Enter') results.querySelector('a')?.click();
  });
  dialog.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });
  document.addEventListener('keydown', event => {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); openSearch(); }
    if (event.key === 'Escape') setMenu(false);
  });
})();
