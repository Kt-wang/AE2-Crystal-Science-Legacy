---
navigation:
  parent: index.md
  title: Basic Pattern Provider
  icon: ae2csl:simple_pattern_provider
  position: 160
item_ids:
  - ae2csl:simple_pattern_provider
  - ae2csl:simple_pattern_provider_part
  - ae2csl:simple_circuit_print
  - ae2csl:simple_processor
  - ae2csl:pattern_provider_upgrade
---

# Basic Pattern Provider

<Row gap="16">
  <BlockImage id="ae2csl:simple_pattern_provider" scale="2" />
  <ItemImage id="ae2csl:simple_pattern_provider_part" scale="2" />
  <ItemImage id="ae2csl:pattern_provider_upgrade" scale="2" />
</Row>

The **Basic Pattern Provider** is an entry-level Pattern Provider offered by AECS.
It introduces pattern-driven automated crafting at a lower cost.
Functionally, it follows the core behavior of a standard Pattern Provider,
with simplified capacity and crafting requirements.

---

## Functional Role

The Basic Pattern Provider is primarily intended for the early stages of an automation setup:

- Provides basic Pattern Provider functionality
- Serves as a lower-cost alternative to the standard Pattern Provider
- Acts as a transitional device

Its behavior is identical to that of a normal Pattern Provider,
with differences mainly in capacity and crafting methods.

---

## Pattern Capacity

The Basic Pattern Provider has a limited number of pattern slots:

- Can hold up to **5 patterns**
- All inserted patterns participate in normal network crafting scheduling

---

## Crafting Characteristics

The crafting path of the Basic Pattern Provider is simplified:

- Requires the use of **<ItemLink id="ae2csl:simple_processor" />**
- Related circuits and processor recipes have a simpler structure
- Does not require Inscriber Presses to complete crafting

---

## Upgrade Path

The Basic Pattern Provider supports expansion through upgrade items:

- Can be upgraded using the **<ItemLink id="ae2csl:pattern_provider_upgrade" />**
- After upgrading, it functions identically to a standard Pattern Provider

---

## Related Recipes

<Row gap="16">
  <Recipe id="ae2csl:inscriber/simple_circuit_print" /> 
  <Recipe id="ae2csl:inscriber/simple_processor" /> 
  <Recipe id="ae2csl:craft/shaped/simple_pattern_provider" /> 
</Row>
