// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app.tools

import java.awt.{ BorderLayout, Color, Dimension, Frame }
import javax.swing.{ BorderFactory, JDialog, JLabel, JPanel, JTabbedPane }

import org.nlogo.core.I18N
import org.nlogo.swing.{ ProgressListener, SwingWorker }

object LibrariesDialog {
  private val BottomPanelBorder = BorderFactory.createCompoundBorder(
    BorderFactory.createEmptyBorder(5, 0, 0, 0),
    BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(2, 0, 0, 0, Color.LIGHT_GRAY),
      BorderFactory.createEmptyBorder(10, 10, 10, 10)))
}

class LibrariesDialog(parent: Frame, categories: Map[String, LibraryInfo => Unit])
extends JDialog(parent, I18N.gui.get("tools.libraries"), false) {
  import LibrariesDialog._

  implicit val i18nPrefix = I18N.Prefix("tools.libraries")

  val tabs = new JTabbedPane
  val bottomPanel = new JPanel
  val status = new JLabel {
    override def getPreferredSize = new Dimension(super.getPreferredSize.width, getFontMetrics(getFont).getHeight)
  }

  val manager = new LibraryManager(categories, new ProgressListener {
    override def start()  = status.setText(I18N.gui("checkingForUpdates"))
    override def finish() = status.setText(null)
  })

  locally {
    manager.listModels.foreach { case (category, contents) =>
      tabs.addTab(I18N.gui("categories." + category),
        new LibrariesTab(contents, lib => {
          status.setText(I18N.gui("installing", lib.name))
          new Installer(category, lib).execute()
        }))
    }

    bottomPanel.setBorder(BottomPanelBorder)
    bottomPanel.add(status)

    setLayout(new BorderLayout)
    add(tabs, BorderLayout.CENTER)
    add(bottomPanel, BorderLayout.SOUTH)
    setSize(550, 400)
  }

  override def setVisible(v: Boolean) = {
    super.setVisible(v)
    if (v) manager.updateMetadataFromGithub()
  }

  class Installer(category: String, lib: LibraryInfo) extends SwingWorker[Any, Any] {
    override def doInBackground() = manager.installer(category)(lib)
    override def onComplete() = {
      manager.reloadMetadata()
      status.setText(null)
    }
  }
}
