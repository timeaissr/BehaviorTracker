"""Check tracked Markdown's local file links and section anchors using the stdlib."""

import re
import subprocess
import sys
import unicodedata
from pathlib import Path
from urllib.parse import unquote, urlsplit


LINK = re.compile(r'!?\[[^\]\n]*\]\(\s*(<[^>\n]+>|[^\s)]+)(?:\s+["\'][^\n]*?["\'])?\s*\)')
REFERENCE = re.compile(r'^ {0,3}\[([^\]]+)\]:\s*(<[^>\n]+>|\S+)', re.MULTILINE)
REFERENCE_LINK = re.compile(r'!?\[([^\]\n]+)\]\[([^\]\n]*)\]')
HEADING = re.compile(r'^ {0,3}#{1,6}\s+(.+?)\s*#*\s*$', re.MULTILINE)
HTML_ANCHOR = re.compile(r'<(?:a|[^\s>]+)\b[^>]*\b(?:id|name)=["\']([^"\']+)["\']', re.IGNORECASE)


def prose(text):
    """Mask fenced code blocks while keeping line numbers for diagnostics."""
    lines = []
    fence = None
    for line in text.splitlines(keepends=True):
        marker = re.match(r'^ {0,3}(`{3,}|~{3,})', line)
        if fence:
            if marker and marker[1][0] == fence[0] and len(marker[1]) >= len(fence):
                fence = None
            lines.append("\n")
        elif marker:
            fence = marker[1]
            lines.append("\n")
        else:
            lines.append(line)
    return "".join(lines)


def anchors(text):
    text = prose(text)
    result = set(HTML_ANCHOR.findall(text))
    generated = set()
    for heading in HEADING.findall(text):
        heading = re.sub(r'!?\[([^\]]+)\]\([^)]*\)', r'\1', heading)
        heading = re.sub(r'<[^>]+>', '', heading).lower()
        slug = ''.join(char for char in heading if char in '-_' or
                       unicodedata.category(char)[0] not in 'PS').replace(' ', '-')
        candidate, count = slug, 0
        while candidate in generated:
            count += 1
            candidate = f'{slug}-{count}'
        generated.add(candidate)
        result.add(candidate)
    return result


def links(text):
    text = prose(text)
    # Link-looking examples in inline code are not links.
    text = re.sub(r'(`+).*?\1', lambda match: ' ' * len(match[0]), text)
    for match in LINK.finditer(text):
        yield text.count('\n', 0, match.start()) + 1, match[1].strip('<>')
    definitions = {match[1].casefold(): match[2].strip('<>')
                   for match in REFERENCE.finditer(text)}
    for match in REFERENCE.finditer(text):
        yield text.count('\n', 0, match.start()) + 1, match[2].strip('<>')
    for match in REFERENCE_LINK.finditer(text):
        key = (match[2] or match[1]).casefold()
        if key not in definitions:
            yield text.count('\n', 0, match.start()) + 1, None


def check_docs(root):
    tracked = subprocess.check_output(['git', 'ls-files', '-z'], cwd=root)
    documents = [root / path.decode('utf-8') for path in tracked.split(b'\0')
                 if path and Path(path.decode('utf-8')).suffix.lower() in {'.md', '.markdown'}]
    anchor_cache = {}
    errors = []
    for document in documents:
        for line, destination in links(document.read_text(encoding='utf-8')):
            problem = None
            if destination is None:
                problem = 'undefined link reference'
            else:
                url = urlsplit(destination)
                if url.scheme or url.netloc:
                    continue
                target = ((root if url.path.startswith('/') else document.parent)
                          / unquote(url.path).lstrip('/')).resolve() if url.path else document
                if not target.exists():
                    problem = f'missing file: {destination}'
                elif url.fragment and target.suffix.lower() in {'.md', '.markdown'}:
                    if target not in anchor_cache:
                        anchor_cache[target] = anchors(target.read_text(encoding='utf-8'))
                    if unquote(url.fragment) not in anchor_cache[target]:
                        problem = f'missing anchor: {destination}'
            if problem:
                errors.append(f'{document.relative_to(root)}:{line}: {problem}')
    for error in errors:
        print(error, file=sys.stderr)
    print(f'Checked {len(documents)} Markdown files; {len(errors)} errors.')
    return not errors


if __name__ == '__main__':
    sys.exit(0 if check_docs(Path.cwd()) else 1)
