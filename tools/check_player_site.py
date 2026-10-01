"""Check the built player guide's navigation, resources and local page links."""
import sys
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit


class Links(HTMLParser):
    def __init__(self):
        super().__init__()
        self.references = []
        self.ids = set()

    def handle_starttag(self, tag, attributes):
        attrs = dict(attributes)
        if "id" in attrs:
            self.ids.add(attrs["id"])
        if tag in ("a", "link", "script", "img"):
            target = attrs.get("src") or attrs.get("href")
            if target:
                self.references.append(target)


def main():
    site = Path(sys.argv[1] if len(sys.argv) > 1 else "_site").resolve()
    failures = []
    checked = 0
    for page in site.rglob("*.html"):
        parser = Links()
        parser.feed(page.read_text(encoding="utf-8"))
        if page.name != "index.html":
            continue
        checked += 1
        if not {"guide-sidebar", "main-content", "search-dialog"}.issubset(parser.ids):
            failures.append(f"Missing guide layout: {page.relative_to(site)}")
        for reference in parser.references:
            url = urlsplit(reference)
            if url.scheme or url.netloc or not url.path:
                continue
            target_path = unquote(url.path)
            if target_path.startswith("/wildercord/"):
                target_path = target_path[len("/wildercord/"):]
                target = site / target_path
            elif target_path.startswith("/"):
                target = site / target_path.lstrip("/")
            else:
                target = page.parent / target_path
            if target.is_dir():
                target = target / "index.html"
            if not target.is_file():
                failures.append(f"Broken resource/link in {page.relative_to(site)}: {reference}")
    if checked < 90:
        failures.append(f"Expected at least 90 rendered guide pages; found {checked}")
    if failures:
        raise SystemExit("\n".join(failures))
    print(f"Verified {checked} rendered pages: navigation, search controls and local resources/links present.")


if __name__ == "__main__":
    main()
