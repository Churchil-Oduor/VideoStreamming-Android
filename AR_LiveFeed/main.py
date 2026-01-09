from fastapi import FastAPI, WebSocket
import cv2 
import numpy as np

app = FastAPI()

@app.websocket("/ws/camera")
async def camera_stream(websocket: WebSocket):
    await websocket.accept()
    print("Android connected")

    try:
        while True:
            data = await websocket.receive_bytes()
            np_arr = np.frombuffer(data, np.uint8)
            frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

            if frame is None:
                print("Failed to decode frame")
                continue
            cv2.putText(frame, "Hello", (10,30),
            cv2.FONT_HERSHEY_SIMPLEX, 1, (0,255,0), 2)

            
            _, encoded = cv2.imencode(".jpg",
                                      frame,
                                      [cv2.IMWRITE_JPEG_QUALITY, 60])

            await websocket.send_bytes(encoded.tobytes())

            cv2.imshow("Camera Frame", frame)

            if cv2.waitKey(1) & 0xFF == ord('q'):
                break

    except Exception as e:
        print("connection closed", e)
    finally:
        cv2.destroyAllWindows()
