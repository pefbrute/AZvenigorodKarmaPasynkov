---
description: How to add new streets or images/coordinates for existing streets
---

This workflow describes the process of adding street-related data to the `zvenigorod_seed.json` file.

### Prerequisites
1.  **Image File**: Ensure the image (e.g., `zveg_streetname_1.png`) is present in `app/src/main/res/drawable`.
2.  **Coordinates**: Obtain coordinates (lat, lon) from a Yandex Maps panorama (look for `panorama[point]=lon,lat` in the URL). *Note: Yandex URLs list lon first, then lat.*

### Step 1: Locating the Data
All street data is stored in `app/src/main/assets/zvenigorod_seed.json` within the `items` array. Streets have `"kind": "street"`.

### Step 2: Adding a New Street
If the street doesn't exist:
1.  Choose a unique ID (e.g., `zvg_street_nekrasova`).
2.  Add a new object at the end of the `items` array:
    ```json
    {
      "id": "zvg_street_identifier",
      "name": "Street Name (in Russian)",
      "kind": "street",
      "group": "streets",
      "lat": lat_value,
      "lon": lon_value,
      "answerRadiusM": 120,
      "geometryData": "[[lat_value, lon_value]]",
      "images": [
        {
          "imageName": "image_base_name",
          "lat": lat_value,
          "lon": lon_value
        }
      ]
    }
    ```

### Step 3: Adding a New Point/Image to an Existing Street
If adding a second or subsequent point to an existing street:
1.  Find the street by its ID or name.
2.  If it has `imageName`, convert it to an `images` array.
3.  Add the new point to the `images` array.
4.  Update `geometryData` (a string containing a JSON array of coordinate pairs) to include the new point in the correct geographic sequence.

### Step 4: Verification
1.  Validate the JSON syntax.
2.  Run `./gradlew installDebug` to deploy the changes to a device/emulator for testing.
