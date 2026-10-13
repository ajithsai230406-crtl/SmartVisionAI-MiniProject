import time

# pyright: reportMissingImports=false
try:
    import pyautogui
except ModuleNotFoundError as exc:
    raise SystemExit(
        "pyautogui is not installed. Install it with: pip install pyautogui"
    ) from exc

# Move the mouse to the top-left corner to abort the script.
pyautogui.FAILSAFE = True

time.sleep(3)  # Focus the target window during this pause.

pyautogui.write("Hello from Python!", interval=0.05)
pyautogui.press("enter")
pyautogui.hotkey("ctrl", "a")  # Example keyboard shortcut