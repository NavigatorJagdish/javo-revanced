"""Inventory common secret exposure paths before changing repository visibility."""
import json
import os
from pathlib import Path
import re
import subprocess


def run(*args):
    return subprocess.check_output(args, text=True, encoding="utf-8", errors="replace")


repo = os.environ["GITHUB_REPOSITORY"]
paths = run("git", "log", "--all", "--name-only", "--format=").splitlines()
suspect_paths = sorted({
    path for path in paths if re.search(r"(?i)(?:^|/)\.env$|\.(?:jks|keystore|p12|pfx|key)$", path)
})
credential_pattern = (
    "-----BEGIN ([A-Z]+ )?PRIVATE KEY-----|"
    "gh[pousr]_[a-zA-Z0-9]{36}|github_pat_[a-zA-Z0-9_]{40,}|AKIA[A-Z0-9]{16}"
)
credential_hits = sorted(set(
    line for line in run("git", "log", "--all", "--name-only", "--format=", "-G", credential_pattern, "--").splitlines()
    if line
))
artifacts = json.loads(run("gh", "api", "--paginate", "--slurp", f"repos/{repo}/actions/artifacts?per_page=100"))
caches = json.loads(run("gh", "api", "--paginate", "--slurp", f"repos/{repo}/actions/caches?per_page=100"))
signing_artifacts = sum(
    item["name"] == "javo-manager-development-identity" and not item["expired"]
    for page in artifacts for item in page["artifacts"]
)
signing_caches = sum(
    item["key"].startswith("javo-manager-development-signing-")
    for page in caches for item in page["actions_caches"]
)
report = {
    "commit": run("git", "rev-parse", "HEAD").strip(),
    "reviewHistoryPaths": suspect_paths,
    "reviewCredentialPatternHits": credential_hits,
    "activeSigningArtifacts": signing_artifacts,
    "signingCaches": signing_caches,
    "requiresManualHistoryAndLogsReview": True,
    "scope": "Git history credential patterns and GitHub Actions signing inventory; not a security certification",
}
Path("evidence").mkdir(exist_ok=True)
output = json.dumps(report, indent=2) + "\n"
Path("evidence/publication-audit.json").write_text(output, encoding="utf-8")
print(output)
