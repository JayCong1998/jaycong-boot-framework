import { categories } from "@/data/products";
export default function CategoryNav({
  selected,
  onSelect,
}: {
  selected: string;
  onSelect: (id: string) => void;
}) {
  return (
    <aside className="category-nav" aria-label="商品分类">
      {categories.map((category) => (
        <button
          key={category.id}
          className={
            "category-item" + (selected === category.id ? " active" : "")
          }
          aria-pressed={selected === category.id}
          disabled={category.id !== "all"}
          title={category.id === "all" ? "全部商品" : "商品分类暂未开放"}
          onClick={() => onSelect(category.id)}
        >
          <category.icon size={23} strokeWidth={1.7} />
          <span>{category.name}</span>
        </button>
      ))}
    </aside>
  );
}
