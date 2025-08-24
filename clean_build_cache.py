import os
import shutil
from pathlib import Path

# ======================================================
# Cache & Build Locations
# ======================================================
CACHE_LOCATIONS = {
    "Android / Gradle": [
        Path.home() / ".gradle" / "caches",
        Path.home() / ".gradle" / "daemon",
        Path.cwd() / "build",
        Path.cwd() / "app" / "build",
        Path.cwd() / ".gradle",
        Path.cwd() / ".idea" / "caches",
        Path.cwd() / ".idea" / "libraries",
        Path.home() / ".android" / "build-cache",
        Path(os.path.expanduser("~\\AppData\\Local\\Gradle\\caches")),
        Path(os.path.expanduser("~\\AppData\\Local\\Gradle\\daemon")),
    ],
    "Node.js": [
        Path.home() / ".npm",
        Path(os.path.expanduser("~\\AppData\\Roaming\\npm-cache")),
        Path.home() / ".cache" / "yarn",
        Path(os.path.expanduser("~\\AppData\\Local\\Yarn\\Cache")),
        Path.home() / ".pnpm-store",
        Path(os.path.expanduser("~\\AppData\\Local\\pnpm-store")),
        Path.cwd() / "node_modules",
        Path.cwd() / "dist",
        Path.cwd() / "build",
        Path.cwd() / ".next",
        Path.cwd() / ".nuxt",
        Path.cwd() / ".svelte-kit",
        Path.cwd() / ".angular",
    ],
    "Maven / Kotlin": [
        Path.home() / ".m2" / "repository",
        Path(os.path.expanduser("~\\.m2\\repository")),
        Path.cwd() / "target",
        Path.cwd() / "build",
        Path.cwd() / ".gradle",
    ],
    "Python": [
        Path.home() / ".cache" / "pip",
        Path(os.path.expanduser("~\\AppData\\Local\\pip\\cache")),
        Path.cwd() / "venv",
        Path.cwd() / ".venv",
        Path.cwd() / "build",
        Path.cwd() / "dist",
    ],
    "Rust (Cargo)": [
        Path.home() / ".cargo" / "registry",
        Path.home() / ".cargo" / "git",
        Path(os.path.expanduser("~\\.cargo\\registry")),
        Path(os.path.expanduser("~\\.cargo\\git")),
        Path.cwd() / "target",
    ],
    "Go": [
        Path.home() / "go" / "pkg" / "mod",
        Path(os.path.expanduser("~\\go\\pkg\\mod")),
        Path.cwd() / "bin",
    ],
    "System Temp": [
        Path("/tmp"),
        Path("/var/tmp"),
        Path(os.getenv("TEMP") or ""),
        Path(os.getenv("TMP") or "")
    ]
}

# ======================================================
# Helpers
# ======================================================
def sizeof_fmt(num, suffix="B"):
    """Convert bytes to human readable format"""
    for unit in ["", "K", "M", "G", "T"]:
        if abs(num) < 1024.0:
            return f"{num:3.1f}{unit}{suffix}"
        num /= 1024.0
    return f"{num:.1f}P{suffix}"

def get_size(path: Path):
    """Calculate folder/file size in bytes"""
    if not path.exists():
        return 0
    if path.is_file():
        return path.stat().st_size
    total = 0
    for root, _, files in os.walk(path, topdown=True):
        for f in files:
            try:
                total += (Path(root) / f).stat().st_size
            except FileNotFoundError:
                pass
    return total

def delete_path_verbose(path: Path):
    """Delete a folder/file and print every step, skip locked files"""
    if not path.exists():
        return 0

    total_freed = 0

    if path.is_file():
        try:
            size = path.stat().st_size
            print(f"   Removing file: {path} ({sizeof_fmt(size)})")
            path.unlink()
            total_freed += size
        except (PermissionError, OSError):
            print(f"   [SKIPPED] In use or access denied: {path}")

    elif path.is_dir():
        for root, dirs, files in os.walk(path, topdown=False):
            for name in files:
                fpath = Path(root) / name
                try:
                    size = fpath.stat().st_size
                    print(f"   Removing file: {fpath} ({sizeof_fmt(size)})")
                    fpath.unlink()
                    total_freed += size
                except (PermissionError, OSError):
                    print(f"   [SKIPPED] In use or access denied: {fpath}")
            for name in dirs:
                dpath = Path(root) / name
                try:
                    dpath.rmdir()
                    print(f"   Removing folder: {dpath}")
                except (OSError, PermissionError):
                    print(f"   [SKIPPED] In use or access denied: {dpath}")
        try:
            shutil.rmtree(path, ignore_errors=True)
            print(f"   Removing root folder: {path}")
        except (OSError, PermissionError):
            print(f"   [SKIPPED] Root folder in use or access denied: {path}")

    return total_freed

def progress_bar(current, total):
    width = 30
    filled = int(width * current / total)
    bar = "#" * filled + "-" * (width - filled)
    return f"[{bar}] {int((current/total)*100)}%"

# ======================================================
# Main Cleaner
# ======================================================
def clean_all_verbose():
    total_cleaned = 0
    categories = list(CACHE_LOCATIONS.keys())
    total_tasks = len(categories)

    for i, (label, paths) in enumerate(CACHE_LOCATIONS.items(), start=1):
        print(f"\n[{i}/{total_tasks}] Cleaning category: {label}")
        for p in paths:
            if p and p.exists():
                size_before = get_size(p)
                if size_before > 0:
                    freed = delete_path_verbose(p)
                    total_cleaned += freed
                    print(f"   [DONE] Finished: {p} ({sizeof_fmt(size_before)}) removed")
        print("   " + progress_bar(i, total_tasks))

    print("\n======================================")
    print(f"Cleanup Finished — Total Space Freed: {sizeof_fmt(total_cleaned)}")
    print("======================================")

# ======================================================
# Run
# ======================================================
if __name__ == "__main__":
    clean_all_verbose()
