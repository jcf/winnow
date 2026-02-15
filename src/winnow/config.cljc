(ns winnow.config
  (:require
   [clojure.string :as str]
   [winnow.parse :as parse]))

(def ^:private length-units
  #{"%"  "ch" "cm" "cqb" "cqh" "cqi" "cqmax" "cqmin" "cqw" "dvh" "dvw"
    "em" "ex" "in" "lh" "lvh" "lvw" "mm" "pc" "pt" "px" "rem" "rlh"
    "svh" "svw" "vh" "vmax" "vmin" "vw"})

(defn- ends-with-unit?
  [s]
  (some #(str/ends-with? s %) length-units))

(defn- stroke-width?
  [s]
  (or (parse-long s)
      (and (parse/arbitrary? s)
           (let [c (parse/arbitrary-content s)]
             (or (ends-with-unit? c)
                 (parse-double c))))))

(def default
  {:colors
   #{"amber"
     "black"
     "blue"
     "current"
     "cyan"
     "emerald"
     "fuchsia"
     "gray"
     "green"
     "indigo"
     "inherit"
     "lime"
     "neutral"
     "orange"
     "pink"
     "purple"
     "red"
     "rose"
     "sky"
     "slate"
     "stone"
     "teal"
     "transparent"
     "violet"
     "white"
     "yellow"
     "zinc"}

   :validators
   {:stroke-width stroke-width?}

   :exact
   {"block"        :display
    "inline-block" :display
    "inline"       :display
    "flex"         :display
    "inline-flex"  :display
    "grid"         :display
    "inline-grid"  :display
    "hidden"       :display
    "contents"     :display
    "flow-root"    :display
    "list-item"    :display

    "visible"   :visibility
    "invisible" :visibility
    "collapse"  :visibility

    "static"   :position
    "fixed"    :position
    "absolute" :position
    "relative" :position
    "sticky"   :position

    "underline"    :text-decoration
    "overline"     :text-decoration
    "line-through" :text-decoration
    "no-underline" :text-decoration

    "ring"       :ring-w
    "shadow"     :shadow
    "inset-ring" :inset-ring-w
    "grow"       :grow
    "shrink"     :shrink

    "normal-nums"        :fvn-reset
    "ordinal"            :fvn-ordinal
    "slashed-zero"       :fvn-slashed-zero
    "lining-nums"        :fvn-figure
    "oldstyle-nums"      :fvn-figure
    "proportional-nums"  :fvn-spacing
    "tabular-nums"       :fvn-spacing
    "diagonal-fractions" :fvn-fraction
    "stacked-fractions"  :fvn-fraction

    "touch-auto"         :touch
    "touch-none"         :touch
    "touch-manipulation" :touch
    "touch-pan-x"        :touch-px
    "touch-pan-left"     :touch-px
    "touch-pan-right"    :touch-px
    "touch-pan-y"        :touch-py
    "touch-pan-up"       :touch-py
    "touch-pan-down"     :touch-py
    "touch-pinch-zoom"   :touch-pz

    "transform-3d"   :transform-style
    "transform-flat" :transform-style

    "rotate-none"      :rotate
    "text-shadow"      :text-shadow
    "text-shadow-none" :text-shadow

    "appearance-none" :appearance
    "appearance-auto" :appearance

    "forced-color-adjust-none" :forced-color-adjust
    "forced-color-adjust-auto" :forced-color-adjust

    "mask-add"       :mask-composite
    "mask-subtract"  :mask-composite
    "mask-intersect" :mask-composite
    "mask-exclude"   :mask-composite

    "bg-auto"    :bg-size
    "bg-cover"   :bg-size
    "bg-contain" :bg-size
    "bg-none"    :bg-image

    "object-contain"    :object-fit
    "object-cover"      :object-fit
    "object-fill"       :object-fit
    "object-none"       :object-fit
    "object-scale-down" :object-fit

    "overflow-clip"    :overflow
    "overflow-visible" :overflow
    "overflow-scroll"  :overflow
    "overflow-auto"    :overflow
    "overflow-hidden"  :overflow

    "overscroll-auto"    :overscroll
    "overscroll-contain" :overscroll
    "overscroll-none"    :overscroll

    "scroll-auto"   :scroll-behavior
    "scroll-smooth" :scroll-behavior

    "truncate"      :text-overflow
    "text-ellipsis" :text-overflow
    "text-clip"     :text-overflow

    "uppercase"   :text-transform
    "lowercase"   :text-transform
    "capitalize"  :text-transform
    "normal-case" :text-transform

    "italic"     :font-style
    "not-italic" :font-style

    "antialiased"          :font-smoothing
    "subpixel-antialiased" :font-smoothing

    "break-normal" :word-break
    "break-words"  :word-break
    "break-all"    :word-break
    "break-keep"   :word-break

    "sr-only"     :sr
    "not-sr-only" :sr

    "isolate"        :isolation
    "isolation-auto" :isolation

    "resize-none" :resize
    "resize-y"    :resize
    "resize-x"    :resize
    "resize"      :resize

    "snap-start"      :snap-align
    "snap-end"        :snap-align
    "snap-center"     :snap-align
    "snap-align-none" :snap-align

    "snap-normal" :snap-stop
    "snap-always" :snap-stop

    "snap-none"      :snap-type
    "snap-x"         :snap-type
    "snap-y"         :snap-type
    "snap-both"      :snap-type
    "snap-mandatory" :snap-strictness
    "snap-proximity" :snap-strictness

    "transition-none"      :transition
    "transition-all"       :transition
    "transition-colors"    :transition
    "transition-opacity"   :transition
    "transition-shadow"    :transition
    "transition-transform" :transition
    "transition-property"  :transition

    "transition-discrete" :transition-behavior
    "transition-normal"   :transition-behavior

    "will-change-auto"      :will-change
    "will-change-scroll"    :will-change
    "will-change-contents"  :will-change
    "will-change-transform" :will-change

    "backface-visible" :backface
    "backface-hidden"  :backface

    "pointer-events-none" :pointer-events
    "pointer-events-auto" :pointer-events

    "list-inside"  :list-style-position
    "list-outside" :list-style-position

    "list-none"    :list-style-type
    "list-disc"    :list-style-type
    "list-decimal" :list-style-type

    "text-left"    :text-align
    "text-center"  :text-align
    "text-right"   :text-align
    "text-justify" :text-align
    "text-start"   :text-align
    "text-end"     :text-align

    "text-wrap"    :text-wrap
    "text-nowrap"  :text-wrap
    "text-pretty"  :text-wrap
    "text-balance" :text-wrap

    "align-baseline"    :vertical-align
    "align-top"         :vertical-align
    "align-middle"      :vertical-align
    "align-bottom"      :vertical-align
    "align-text-top"    :vertical-align
    "align-text-bottom" :vertical-align
    "align-sub"         :vertical-align
    "align-super"       :vertical-align

    "bg-fixed"  :bg-attachment
    "bg-local"  :bg-attachment
    "bg-scroll" :bg-attachment

    "bg-clip-border"  :bg-clip
    "bg-clip-padding" :bg-clip
    "bg-clip-content" :bg-clip
    "bg-clip-text"    :bg-clip

    "bg-origin-border"  :bg-origin
    "bg-origin-padding" :bg-origin
    "bg-origin-content" :bg-origin

    "bg-repeat"       :bg-repeat
    "bg-no-repeat"    :bg-repeat
    "bg-repeat-x"     :bg-repeat
    "bg-repeat-y"     :bg-repeat
    "bg-repeat-round" :bg-repeat
    "bg-repeat-space" :bg-repeat

    "border-solid"  :border-style
    "border-dashed" :border-style
    "border-dotted" :border-style
    "border-double" :border-style
    "border-hidden" :border-style
    "border-none"   :border-style

    "divide-solid"  :divide-style
    "divide-dashed" :divide-style
    "divide-dotted" :divide-style
    "divide-double" :divide-style
    "divide-none"   :divide-style

    "border-collapse" :border-collapse
    "border-separate" :border-collapse

    "table-auto"  :table-layout
    "table-fixed" :table-layout

    "flex-row"         :flex-direction
    "flex-row-reverse" :flex-direction
    "flex-col"         :flex-direction
    "flex-col-reverse" :flex-direction

    "flex-wrap"         :flex-wrap
    "flex-wrap-reverse" :flex-wrap
    "flex-nowrap"       :flex-wrap

    "flex-1"       :flex
    "flex-auto"    :flex
    "flex-initial" :flex
    "flex-none"    :flex

    "grid-flow-row"       :grid-flow
    "grid-flow-col"       :grid-flow
    "grid-flow-dense"     :grid-flow
    "grid-flow-row-dense" :grid-flow
    "grid-flow-col-dense" :grid-flow

    "box-border"  :box-sizing
    "box-content" :box-sizing

    "decoration-solid"  :decoration-style
    "decoration-double" :decoration-style
    "decoration-dotted" :decoration-style
    "decoration-dashed" :decoration-style
    "decoration-wavy"   :decoration-style

    "inert" :inert}

   :prefixes
   {"p"  {:group :p}
    "px" {:group :px}
    "py" {:group :py}
    "pt" {:group :pt}
    "pr" {:group :pr}
    "pb" {:group :pb}
    "pl" {:group :pl}
    "ps" {:group :ps}
    "pe" {:group :pe}

    "m"   {:group :m}
    "-m"  {:group :m}
    "mx"  {:group :mx}
    "-mx" {:group :mx}
    "my"  {:group :my}
    "-my" {:group :my}
    "mt"  {:group :mt}
    "-mt" {:group :mt}
    "mr"  {:group :mr}
    "-mr" {:group :mr}
    "mb"  {:group :mb}
    "-mb" {:group :mb}
    "ml"  {:group :ml}
    "-ml" {:group :ml}
    "ms"  {:group :ms}
    "-ms" {:group :ms}
    "me"  {:group :me}
    "-me" {:group :me}

    "w"     {:group :w}
    "h"     {:group :h}
    "min-w" {:group :min-w}
    "min-h" {:group :min-h}
    "max-w" {:group :max-w}
    "max-h" {:group :max-h}
    "size"  {:group :size}

    "inset"    {:group :inset}
    "-inset"   {:group :inset}
    "inset-x"  {:group :inset-x}
    "-inset-x" {:group :inset-x}
    "inset-y"  {:group :inset-y}
    "-inset-y" {:group :inset-y}
    "top"      {:group :top}
    "-top"     {:group :top}
    "right"    {:group :right}
    "-right"   {:group :right}
    "bottom"   {:group :bottom}
    "-bottom"  {:group :bottom}
    "left"     {:group :left}
    "-left"    {:group :left}
    "start"    {:group :start}
    "-start"   {:group :start}
    "end"      {:group :end}
    "-end"     {:group :end}

    "z"  {:group :z}
    "-z" {:group :z}

    "basis"  {:group :basis}
    "grow"   {:group :grow}
    "shrink" {:group :shrink}

    "gap"   {:group :gap}
    "gap-x" {:group :gap-x}
    "gap-y" {:group :gap-y}

    "space-x"         {:group :space-x}
    "space-x-reverse" {:group :space-x}
    "space-y"         {:group :space-y}
    "space-y-reverse" {:group :space-y}

    "divide-x"         {:group :divide-x}
    "divide-x-reverse" {:group :divide-x}
    "divide-y"         {:group :divide-y}
    "divide-y-reverse" {:group :divide-y}
    "divide"           {:validators [[:color :divide-color]
                                     [:border-width :divide-w]]}

    "aspect"  {:group :aspect}
    "columns" {:group :columns}

    "object" {:group :object-position}

    "scroll-m"  {:group :scroll-m}
    "scroll-mx" {:group :scroll-mx}
    "scroll-my" {:group :scroll-my}
    "scroll-ms" {:group :scroll-ms}
    "scroll-me" {:group :scroll-me}
    "scroll-mt" {:group :scroll-mt}
    "scroll-mr" {:group :scroll-mr}
    "scroll-mb" {:group :scroll-mb}
    "scroll-ml" {:group :scroll-ml}

    "scroll-p"  {:group :scroll-p}
    "scroll-px" {:group :scroll-px}
    "scroll-py" {:group :scroll-py}
    "scroll-ps" {:group :scroll-ps}
    "scroll-pe" {:group :scroll-pe}
    "scroll-pt" {:group :scroll-pt}
    "scroll-pr" {:group :scroll-pr}
    "scroll-pb" {:group :scroll-pb}
    "scroll-pl" {:group :scroll-pl}

    "accent"      {:group :accent}
    "caret"       {:validators [[:color-or-var :caret-color]]}
    "fill"        {:validators [[:color-or-var :fill]]}
    "placeholder" {:validators [[:color-or-var :placeholder-color]]}

    "indent"           {:group :indent}
    "animate"          {:group :animate}
    "ease"             {:group :ease}
    "underline-offset" {:group :underline-offset}
    "decoration"       {:validators [[:color :decoration-color]
                                     [:decoration-thickness :decoration-thickness]]}

    "border-spacing"   {:group :border-spacing}
    "border-spacing-x" {:group :border-spacing-x}
    "border-spacing-y" {:group :border-spacing-y}

    "auto-cols" {:group :auto-cols}
    "auto-rows" {:group :auto-rows}
    "col-start" {:group :col-start}
    "col-end"   {:group :col-end}
    "row-start" {:group :row-start}
    "row-end"   {:group :row-end}

    "blur"        {:group :blur}
    "brightness"  {:group :brightness}
    "contrast"    {:group :contrast}
    "saturate"    {:group :saturate}
    "sepia"       {:group :sepia}
    "invert"      {:group :invert}
    "hue-rotate"  {:group :hue-rotate}
    "-hue-rotate" {:group :hue-rotate}

    "backdrop-blur"       {:group :backdrop-blur}
    "backdrop-brightness" {:group :backdrop-brightness}
    "backdrop-contrast"   {:group :backdrop-contrast}
    "backdrop-grayscale"  {:group :backdrop-grayscale}
    "backdrop-hue-rotate" {:group :backdrop-hue-rotate}
    "backdrop-invert"     {:group :backdrop-invert}
    "backdrop-opacity"    {:group :backdrop-opacity}
    "backdrop-saturate"   {:group :backdrop-saturate}
    "backdrop-sepia"      {:group :backdrop-sepia}

    "transform-origin" {:group :transform-origin}

    "overflow"   {:group :overflow}
    "overflow-x" {:group :overflow-x}
    "overflow-y" {:group :overflow-y}

    "bg"        {:validators [[:color-or-var :bg-color]
                              [:position :bg-position]
                              [:image :bg-image]
                              [:length :bg-size]]}
    "bg-linear" {:group :gradient}
    "bg-radial" {:group :gradient}
    "bg-conic"  {:group :gradient}
    "from"      {:validators [[:number :gradient-from-pos]
                              [:percent :gradient-from-pos]
                              [:length :gradient-from-pos]
                              [:position :gradient-from-pos]
                              [:color-or-var :gradient-from]]}
    "via"       {:validators [[:number :gradient-via-pos]
                              [:percent :gradient-via-pos]
                              [:length :gradient-via-pos]
                              [:position :gradient-via-pos]
                              [:color-or-var :gradient-via]]}
    "to"        {:validators [[:number :gradient-to-pos]
                              [:percent :gradient-to-pos]
                              [:length :gradient-to-pos]
                              [:position :gradient-to-pos]
                              [:color-or-var :gradient-to]]}

    "ring"       {:validators [[:color :ring-color]
                               [:ring-width :ring-w]]}
    "inset-ring" {:validators [[:color :inset-ring-color]
                               [:ring-width :inset-ring-w]]}

    "shadow"      {:validators [[:color :shadow-color]
                                [:shadow-size :shadow]]}
    "text-shadow" {:validators [[:color :text-shadow-color]
                                [:shadow-size :text-shadow]]}
    "drop-shadow" {:validators [[:color :drop-shadow-color]
                                [:shadow-size :drop-shadow]]}

    "mix-blend" {:group :mix-blend}

    "stroke"  {:validators [[:stroke-width :stroke-w]
                            [:color-or-var :stroke-color]]}
    "outline" {:validators [[:integer :outline-w]
                            [:color-or-var :outline-color]]}

    "border"   {:validators [[:color :border-color]
                             [:border-width :border-w]]}
    "border-x" {:validators [[:color :border-x-color]
                             [:border-width :border-x-w]]}
    "border-y" {:validators [[:color :border-y-color]
                             [:border-width :border-y-w]]}
    "border-t" {:validators [[:color :border-t-color]
                             [:border-width :border-t-w]]}
    "border-r" {:validators [[:color :border-r-color]
                             [:border-width :border-r-w]]}
    "border-b" {:validators [[:color :border-b-color]
                             [:border-width :border-b-w]]}
    "border-l" {:validators [[:color :border-l-color]
                             [:border-width :border-l-w]]}
    "border-s" {:validators [[:color :border-s-color]
                             [:border-width :border-s-w]]}
    "border-e" {:validators [[:color :border-e-color]
                             [:border-width :border-e-w]]}

    "col-span"  {:group :col-span}
    "col"       {:group :col}
    "row-span"  {:group :row-span}
    "row"       {:group :row}
    "grid-cols" {:group :grid-cols}
    "grid-rows" {:group :grid-rows}

    "grayscale" {:group :grayscale}
    "opacity"   {:group :opacity}
    "scale"     {:group :scale}

    "rotate"   {:group :rotate}
    "-rotate"  {:group :rotate}
    "rotate-x" {:group :rotate-x}
    "rotate-y" {:group :rotate-y}
    "rotate-z" {:group :rotate-z}

    "translate-x"  {:group :translate-x}
    "-translate-x" {:group :translate-x}
    "translate-y"  {:group :translate-y}
    "-translate-y" {:group :translate-y}
    "translate-z"  {:group :translate-z}
    "-translate-z" {:group :translate-z}

    "skew-x"  {:group :skew-x}
    "-skew-x" {:group :skew-x}
    "skew-y"  {:group :skew-y}
    "-skew-y" {:group :skew-y}

    "scale-x" {:group :scale-x}
    "scale-y" {:group :scale-y}
    "scale-z" {:group :scale-z}

    "perspective"        {:group :perspective}
    "perspective-origin" {:group :perspective-origin}

    "text"       {:validators [[:color-or-var :text-color]
                               [:text-size :text-size]]}
    "line-clamp" {:group :line-clamp}
    "leading"    {:group :leading}
    "tracking"   {:group :tracking}

    "font"         {:validators [[:family :font-family]
                                 [:font-weight :font-weight]]}
    "font-stretch" {:group :font-stretch}

    "cursor" {:group :cursor}
    "select" {:group :select}
    "order"  {:group :order}
    "-order" {:group :order}

    "rounded"    {:group :rounded}
    "rounded-t"  {:group :rounded-t}
    "rounded-r"  {:group :rounded-r}
    "rounded-b"  {:group :rounded-b}
    "rounded-l"  {:group :rounded-l}
    "rounded-s"  {:group :rounded-s}
    "rounded-e"  {:group :rounded-e}
    "rounded-tl" {:group :rounded-tl}
    "rounded-tr" {:group :rounded-tr}
    "rounded-bl" {:group :rounded-bl}
    "rounded-br" {:group :rounded-br}
    "rounded-ss" {:group :rounded-ss}
    "rounded-se" {:group :rounded-se}
    "rounded-es" {:group :rounded-es}
    "rounded-ee" {:group :rounded-ee}

    "float"      {:group :float}
    "clear"      {:group :clear}
    "whitespace" {:group :whitespace}
    "hyphens"    {:group :hyphens}
    "caption"    {:group :caption}
    "list-image" {:group :list-image}
    "delay"      {:group :delay}
    "duration"   {:group :duration}

    "justify"       {:group :justify}
    "content"       {:group :content}
    "place-content" {:group :place-content}
    "items"         {:group :items}
    "place-items"   {:group :place-items}
    "self"          {:group :self}
    "place-self"    {:group :place-self}

    "wrap"         {:group :wrap}
    "scheme"       {:group :scheme}
    "field-sizing" {:group :field-sizing}
    "mask-type"    {:group :mask-type}

    "mask-t-from"      {:group :mask-t-from}
    "mask-t-to"        {:group :mask-t-to}
    "mask-r-from"      {:group :mask-r-from}
    "mask-r-to"        {:group :mask-r-to}
    "mask-b-from"      {:group :mask-b-from}
    "mask-b-to"        {:group :mask-b-to}
    "mask-l-from"      {:group :mask-l-from}
    "mask-l-to"        {:group :mask-l-to}
    "mask-radial"      {:group :mask-radial}
    "mask-radial-at"   {:group :mask-radial-at}
    "mask-radial-from" {:group :mask-radial-from}
    "mask-radial-to"   {:group :mask-radial-to}

    "has"       {:group :has}
    "group-has" {:group :group-has}}

   :conflicts
   {:p        [:px :py :pt :pr :pb :pl :ps :pe]
    :px       [:pr :pl]
    :py       [:pt :pb]
    :m        [:mx :my :mt :mr :mb :ml :ms :me]
    :mx       [:mr :ml]
    :my       [:mt :mb]
    :inset    [:inset-x :inset-y :start :end :top :right :bottom :left]
    :inset-x  [:right :left :start :end]
    :inset-y  [:top :bottom]
    :size     [:w :h]
    :gap      [:gap-x :gap-y]
    :overflow [:overflow-x :overflow-y]

    :border-color   [:border-t-color :border-r-color :border-b-color :border-l-color
                     :border-x-color :border-y-color :border-s-color :border-e-color]
    :border-x-color [:border-r-color :border-l-color]
    :border-y-color [:border-t-color :border-b-color]

    :rounded   [:rounded-t :rounded-r :rounded-b :rounded-l :rounded-s :rounded-e
                :rounded-tl :rounded-tr :rounded-bl :rounded-br
                :rounded-ss :rounded-se :rounded-es :rounded-ee]
    :rounded-t [:rounded-tl :rounded-tr]
    :rounded-r [:rounded-tr :rounded-br]
    :rounded-b [:rounded-bl :rounded-br]
    :rounded-l [:rounded-tl :rounded-bl]
    :rounded-s [:rounded-ss :rounded-es]
    :rounded-e [:rounded-se :rounded-ee]

    :fvn-reset        [:fvn-ordinal :fvn-slashed-zero :fvn-figure :fvn-spacing :fvn-fraction]
    :fvn-ordinal      [:fvn-reset]
    :fvn-slashed-zero [:fvn-reset]
    :fvn-figure       [:fvn-reset]
    :fvn-spacing      [:fvn-reset]
    :fvn-fraction     [:fvn-reset]

    :touch    [:touch-px :touch-py :touch-pz]
    :touch-px [:touch]
    :touch-py [:touch]
    :touch-pz [:touch]

    :line-clamp [:display :overflow]

    :gradient [:bg-image]
    :bg-image [:gradient]

    :col      [:col-span]
    :col-span [:col]
    :row      [:row-span]
    :row-span [:row]}

   :order-sensitive-modifiers ["*" "[" "group" "peer"]})
