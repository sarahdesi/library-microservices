import * as React from "react"
import { Tabs as TabsPrimitive } from "radix-ui"

import { cn } from "@/lib/utils"

function Tabs({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.Root>) {
  return <TabsPrimitive.Root data-slot="tabs" className={cn("flex flex-col", className)} {...props} />
}

/** Underlined tabs: text only, the active one marked by a 2px line in the accent color */
function TabsList({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.List>) {
  return <TabsPrimitive.List data-slot="tabs-list" className={cn("flex gap-5 border-b", className)} {...props} />
}

function TabsTrigger({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.Trigger>) {
  return (
    <TabsPrimitive.Trigger
      data-slot="tabs-trigger"
      className={cn(
        "-mb-px inline-flex items-center gap-1.5 border-b-2 border-transparent pb-2.5 text-muted-foreground transition-colors duration-150 outline-none hover:text-foreground focus-visible:text-foreground data-[state=active]:border-primary data-[state=active]:font-medium data-[state=active]:text-foreground",
        className,
      )}
      {...props}
    />
  )
}

function TabsContent({ className, ...props }: React.ComponentProps<typeof TabsPrimitive.Content>) {
  return <TabsPrimitive.Content data-slot="tabs-content" className={cn("pt-4 outline-none", className)} {...props} />
}

export { Tabs, TabsList, TabsTrigger, TabsContent }
