---
navigation:
  title: Modular Fan
  icon: fan
  position: 20
categories:
- automation
item_ids:
- iska_utils:fan
---

# Modular Fan

<BlockImage id="fan" scale="8" />

The Modular Fan is an advanced block that pushes or pulls entities (mobs and players) in a specific direction. It is highly customizable through upgrade modules and offers complete control over its area of effect and power.

## Key Features

- **Push/Pull Mode**: Can either push or pull entities
- **Customizable Area**: Control the area of effect in all directions (up, down, left, right, forward)
- **Upgrade System**: Three slots for modules that enhance the fan's capabilities
- **Advanced Redstone Control**: Five different modes to integrate the fan into your redstone circuits

## How to Use

### Controlling the Area of Effect

The GUI shows a 5x5 grid representing the fan's area of effect. You can adjust the area using the + and - buttons around the grid:

- **Top Buttons**: Adjust the range upward
- **Bottom Buttons**: Adjust the range downward
- **Left Buttons**: Adjust the range to the left
- **Right Buttons**: Adjust the range to the right
- **Bottom Bar**: Adjust the forward range (depth)

The bottom bar visually shows the forward range with colored squares:
- **Green**: Normal range (representable in the bar)
- **Blue**: Extended range (beyond visual representation)

### Push/Pull Mode

The button in the GUI allows you to toggle between:
- **Push**: Pushes entities in the fan's direction
- **Pull**: Pulls entities toward the fan (opposite direction)

### Push Type

You can choose which entities are affected:
- **Mobs Only**: Pushes only mobs, not players
- **Mobs and Players**: Pushes both mobs and players
- **Players Only**: Pushes only players

### Redstone Control

The fan supports five redstone modes:

- **Ignore**: The fan is always active, completely ignores redstone signals
- **Low**: The fan activates only when there is **no** redstone signal
- **High**: The fan activates only when there **is** a redstone signal
- **Disabled**: The fan is always disabled, regardless of redstone

Switch between modes by clicking the button in the GUI. The tooltip always shows the current mode when you hover over the button.

### Area Visualization

The "Show" button in the GUI allows you to visualize the fan's area of effect for several seconds:

- **Purple Markers**: Indicate air blocks at the edges of the area (where air can pass)
- **Red Markers**: Indicate solid blocks at the edges or obstacles inside the area

This helps you understand exactly where the fan can push entities and where there are obstacles.

## Upgrade System

The fan can be upgraded with modules that enhance its capabilities:

### Range Module

Increases the maximum range in all directions. Each range module adds one extra block of range to all parameters (up, down, left, right, forward). The maximum number of range modules that can be installed is configurable.

### Ghost Module

Allows the fan to push entities **through solid blocks**. Normally, if there's a block between the fan and an entity, the airflow is blocked and the entity won't be pushed. With the Ghost Module installed, this limitation is removed.

You can install only **one** Ghost Module per fan.

### Acceleration Modules

Increase the fan's power, making the push stronger. There are different levels of acceleration modules (slow, moderate, fast, extreme, ultra), each with different power. The maximum number of acceleration modules that can be installed is configurable.

The fan's final power is the sum of the base power plus the power of all installed acceleration modules.

## Wind Mechanics

The fan creates an airflow in the direction it's facing. This airflow:

- **Can be blocked**: If there's a solid block between the fan and an entity, the wind doesn't pass and the entity won't be pushed (unless the Ghost Module is installed)
- **Has an area of effect**: Only entities within the configured area are affected

## Tips

- Use the area visualization ("Show") to plan fan placement
- The Ghost Module is useful when you want to push entities through walls or obstacles
- Combine multiple acceleration modules for very powerful pushes
- Use range modules to create very wide areas of effect
- Redstone control "Low" is useful to turn off the fan when there's a redstone signal
- Redstone control "High" is useful to activate the fan only when needed
