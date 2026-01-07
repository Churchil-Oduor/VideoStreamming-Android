# ws_camera_test.py
import asyncio
import cv2
import websockets

SERVER_URI = "ws://localhost:8000/ws/camera"

async def send_frames():
    async with websockets.connect(SERVER_URI, max_size=None) as ws:
        cap = cv2.VideoCapture(0)

        if not cap.isOpened():
            print("❌ Cannot open webcam")
            return

        print("📤 Streaming frames...")

        while True:
            ret, frame = cap.read()
            if not ret:
                break

            # Resize to match Android pipeline
            frame = cv2.resize(frame, (640, 480))

            # Encode as JPEG
            _, jpeg = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, 60])

            await ws.send(jpeg.tobytes())

            await asyncio.sleep(1 / 15)  # 15 FPS

asyncio.run(send_frames())

