# Bilder

[![](https://jitpack.io/v/kshitij6325/BilderDemo.svg)](https://jitpack.io/#kshitij6325/bilder)
![](https://camo.githubusercontent.com/268662b5c12f3076813bc0ea797ccf04921ca28eb30936492a0e82d2b6ddb1de/68747470733a2f2f696d672e736869656c64732e696f2f62616467652f6c616e67756167652d6b6f746c696e2d626c75653f6c6f676f3d6b6f746c696e)

## Download
Add following to your project's build.gradle
```
allprojects {
  repositories {
      ...			
    maven { url 'https://jitpack.io' }
  }
}
```
Add following to your module's build.gralde
```
dependencies {
  implementation 'com.github.kshitij6325:bilder:latest-version'
}
```
## How to use?
```
override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Bilder.init(holder.image.context)
            .load(Source.Url(list[position].second), imageView = holder.image)
    }


```
