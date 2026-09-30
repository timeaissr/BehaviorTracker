"""Choose the CI path from the complete PR or push diff, without API permissions."""

import json
import os
import re
import subprocess
from pathlib import PurePosixPath


def git(*args):
    return subprocess.check_output(["git", *args], stderr=subprocess.PIPE)


def is_documentation(path):
    # Build inputs and CI must always take the full path, even if named *.md.
    if path.startswith(("app/", "gradle/", ".github/", "scripts/")):
        return False
    return path.startswith("docs/") or PurePosixPath(path).suffix.lower() in {
        ".md", ".markdown"
    }


def detect_changes(event_name, event):
    if event_name == "workflow_dispatch":
        print("Manual run: use the full build.")
        return False, "", ""

    try:
        if event_name == "pull_request":
            base = event["pull_request"]["base"]["sha"]
            head = event["pull_request"]["head"]["sha"]
        elif event_name == "push":
            base, head = event["before"], event["after"]
        else:
            raise ValueError("unsupported event")

        if not all(re.fullmatch(r"[0-9a-fA-F]{40}", sha) and int(sha, 16)
                   for sha in (base, head)):
            raise ValueError("missing comparison commit")

        if event_name == "pull_request":
            base = git("merge-base", base, head).decode().strip()

        # Disable rename detection so a code-to-doc move also includes the old path.
        paths = [os.fsdecode(path) for path in git(
            "diff", "--no-ext-diff", "--no-renames", "--name-only", "-z",
            base, head, "--"
        ).split(b"\0") if path]
        docs_only = bool(paths) and all(is_documentation(path) for path in paths)
        print(f"Changed files: {len(paths)}; documentation only: {docs_only}")
        return docs_only, base, head
    except (KeyError, TypeError, ValueError, subprocess.CalledProcessError) as error:
        print(f"Cannot determine the change range ({type(error).__name__}); use the full build.")
        return False, "", ""


def main():
    with open(os.environ["GITHUB_EVENT_PATH"], encoding="utf-8") as event_file:
        event = json.load(event_file)
    docs_only, base, head = detect_changes(os.environ["GITHUB_EVENT_NAME"], event)
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write(f"docs_only={str(docs_only).lower()}\n")
        output.write(f"base_sha={base}\nhead_sha={head}\n")


if __name__ == "__main__":
    main()
