from __future__ import annotations
import time
from collections import deque

class RateLimiter:
    def __init__(self, rpm: int):
        self.rpm = max(1, rpm)
        self.window = 60.0
        self.calls = deque()

    def wait(self):
        now = time.time()
        while self.calls and now - self.calls[0] >= self.window:
            self.calls.popleft()
        if len(self.calls) >= self.rpm:
            sleep_for = self.window - (now - self.calls[0]) + 0.05
            if sleep_for > 0:
                print(f"[rate-limit] aguardando {sleep_for:.1f}s (RPM={self.rpm})...")
                time.sleep(sleep_for)
            return self.wait()
        self.calls.append(time.time())
