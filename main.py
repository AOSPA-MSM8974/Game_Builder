import time
import json
import os
import random

from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.clock import Clock


SAVE_FILE = "kingdom_save.json"


class KingdomGame(App):

    def build(self):

        # =====================
        # Game State
        # =====================
        self.gold = 100
        self.gold_per_sec = 2
        self.last_save_time = time.time()

        self.load_game()

        # =====================
        # UI Layout
        # =====================
        layout = BoxLayout(orientation="vertical", padding=20, spacing=10)

        self.label = Label(
            text="🏰 Kingdom Loading...",
            font_size=40
        )
        layout.add_widget(self.label)

        # =====================
        # Buttons
        # =====================

        btn_collect = Button(
            text="💰 Collect Gold",
            font_size=28
        )
        btn_collect.bind(on_press=self.collect_gold)
        layout.add_widget(btn_collect)

        btn_raid = Button(
            text="⚔️ Raid Village",
            font_size=28
        )
        btn_raid.bind(on_press=self.raid)
        layout.add_widget(btn_raid)

        # =====================
        # Idle loop
        # =====================
        Clock.schedule_interval(self.game_loop, 1)

        self.update_ui()

        return layout

    # =====================
    # Idle income system
    # =====================
    def game_loop(self, dt):
        self.gold += self.gold_per_sec
        self.update_ui()

    # =====================
    # Manual actions
    # =====================
    def collect_gold(self, instance):
        self.gold += 10
        self.update_ui()

    def raid(self, instance):

        if random.random() > 0.4:
            loot = random.randint(40, 120)
            self.gold += loot
        else:
            loss = random.randint(10, 30)
            self.gold = max(0, self.gold - loss)

        self.update_ui()

    # =====================
    # UI Update
    # =====================
    def update_ui(self):
        self.label.text = f"""
🏰 Medieval Kingdom

🪙 Gold: {self.gold}
⚡ +{self.gold_per_sec}/sec
"""

    # =====================
    # Offline system
    # =====================
    def on_start(self):
        self.apply_offline_income()

    def apply_offline_income(self):

        if os.path.exists(SAVE_FILE):
            with open(SAVE_FILE, "r") as f:
                data = json.load(f)

            self.gold = data.get("gold", 100)
            last_time = data.get("last_time", time.time())

            # Calculate offline time
            diff = time.time() - last_time

            offline_gold = int(diff * self.gold_per_sec)

            self.gold += offline_gold

    def on_stop(self):
        self.save_game()

    # =====================
    # Save system
    # =====================
    def save_game(self):

        data = {
            "gold": self.gold,
            "last_time": time.time()
        }

        with open(SAVE_FILE, "w") as f:
            json.dump(data, f)

    # =====================
    # Start app
    # =====================
if __name__ == "__main__":
    KingdomGame().run()
